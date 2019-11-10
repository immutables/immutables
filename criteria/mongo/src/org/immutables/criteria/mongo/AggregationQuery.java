/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.mongo;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Sorts;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.criteria.backend.ExpressionNaming;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.backend.UniqueCachedNaming;
import org.immutables.criteria.expression.AggregationOperators;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class AggregationQuery {

  private final Query query;
  private final PathNaming pathNaming;
  private final ExpressionNaming projectionNaming;
  private final BiMap<Expression, String> naming;
  private final CodecRegistry registry;

  AggregationQuery(Query query, PathNaming pathNaming) {
    this.query = maybeRewriteDistinctToGroupBy(query);
    this.pathNaming = Objects.requireNonNull(pathNaming, "naming");

    BiMap<Expression, String> biMap = HashBiMap.create();

    List<Path> paths = Stream.concat(query.projections().stream(), Stream.concat(query.groupBy().stream(), query.collations().stream().map(Collation::expression)))
            .map(AggregationQuery::extractPath).collect(Collectors.toList());

    @SuppressWarnings("unchecked")
    ExpressionNaming naming = ExpressionNaming.of(UniqueCachedNaming.of(paths.iterator()));
    paths.forEach(p -> biMap.put(p, naming.name(p)));

    this.projectionNaming = ExpressionNaming.of(UniqueCachedNaming.of(query.projections()));
    this.naming = ImmutableBiMap.copyOf(biMap);
    this.registry = MongoClientSettings.getDefaultCodecRegistry();
  }

  /**
   * Converts query of type {@code select distinct a, b, c from table} to {@code select a, b, c from table group by a, b, c}
   * so same pipelines (like {@link Group}) can be (re-)used. Does nothing if this is not a {@code distinct} query.
   * @param query query to be converted
   * @return new version of the query with projections copied to {@code groupBy} list or unmodified query.
   * @throws UnsupportedOperationException if query is "distinct" and {@code groupBy} is not empty
   */
  private static Query maybeRewriteDistinctToGroupBy(Query query) {
    Objects.requireNonNull(query, "query");
    if (!query.distinct()) {
      return query;
    }
    if (query.distinct() && !query.groupBy().isEmpty()) {
      throw new UnsupportedOperationException(String.format("Both DISTINCT and groupBy (%s) are present. Use either.", query.groupBy()));
    }

    // "select distinct a, b, c from table"  equivalent to "select a, b, c from table group by a, b, c"
    return ImmutableQuery.builder().from(query).groupBy(query.projections()).build();
  }

  private static Path extractPath(Expression expression) {
    if (expression instanceof Path) {
      return (Path) expression;
    }

    if (expression instanceof Call) {
      Call call = (Call) expression;
      if (call.operator().arity() != Operator.Arity.UNARY) {
        throw new IllegalArgumentException("Expected unary operator but got " + call);
      }

      Expression arg = call.arguments().get(0);
      Preconditions.checkArgument(arg instanceof Path, "expected path got %s", arg);

      return (Path) arg;
    }

    throw new IllegalArgumentException("Can't extract path from " + expression);
  }

  List<Bson> toPipeline() {
    final List<Bson> aggregates = new ArrayList<>();
    final List<Pipeline> pipelines = new ArrayList<>(Arrays.asList(new MatchPipeline(), new NameAndExtractFields(),
            new Group(), new Sort(), new Skip(), new Limit(), new CountAll()));
    pipelines.forEach(p -> p.process(aggregates::add));
    return Collections.unmodifiableList(aggregates);
  }

  private interface Pipeline {
    void process(Consumer<Bson> consumer);
  }

  private class CountAll implements Pipeline {
    @Override
    public void process(Consumer<Bson> consumer) {
      if (!query.count()) {
        // skip. Valid only for countAll
        return;
      }
      BsonDocument bson = new BsonDocument();
      bson.put("$count", new BsonString("count"));
      consumer.accept(bson);
    }
  }

  private class MatchPipeline implements Pipeline {
    @Override
    public void process(Consumer<Bson> consumer) {
      query.filter().ifPresent(expr -> {
        Bson filter = expr.accept(new FindVisitor(pathNaming));
        Objects.requireNonNull(filter, "null filter");
        consumer.accept(Aggregates.match(filter));
      });
    }
  }

  private class NameAndExtractFields implements Pipeline {
    @Override
    public void process(Consumer<Bson> consumer) {
      BsonDocument projections = new BsonDocument();
      naming.forEach((expr, name) -> {
        projections.put(name, new BsonString("$" + pathNaming.name(extractPath(expr))));
      });

      if (!projections.isEmpty()) {
        consumer.accept(Aggregates.project(projections));
      }
    }
  }

  private class Group implements Pipeline {

    @Override
    public void process(Consumer<Bson> consumer) {
      // {$group: {_id: {STATE: '$STATE', CITY: '$CITY'}, C: {$sum: 1}}}
      // {$project: {_id: 0, STATE: '$_id.STATE', CITY: '$_id.CITY', C: '$C'}}
      final BsonDocument id = new BsonDocument();
      final List<BsonField> accumulators = new ArrayList<>();
      final BsonDocument project = new BsonDocument();

      // $group
      query.groupBy().forEach(groupBy -> {
        String alias = naming.get(extractPath(groupBy));
        id.put(alias, new BsonString("$" + alias));
      });

      // $project part
      for (Expression expr: query.projections()) {
        final String alias = naming.get(extractPath(expr));
        final String uniq = projectionNaming.name(expr);
        if (query.groupBy().contains(expr)) {
          project.put(alias, new BsonString(id.size() == 1 ? "$_id" : "$_id." + alias));
        } else if (expr instanceof Call) {
          // actual aggregation sum / max / min count etc.
          accumulators.add(accumulator(uniq, expr));
          project.put(uniq, new BsonString("$" + uniq));
        } else {
          // simple projection
          project.put(uniq, new BsonString("$" + uniq));
        }
      }

      if (query.distinct() && !id.isEmpty()) {
        BsonValue groupId = id.size() == 1 ? id.values().iterator().next() : id;
        consumer.accept(Aggregates.group(groupId));
      }

      if (!accumulators.isEmpty()) {
        BsonValue groupId = id.size() == 1 ? id.values().iterator().next() : id;
        consumer.accept(Aggregates.group(groupId, accumulators));
      }

      if (!project.isEmpty()) {
        consumer.accept(Aggregates.project(project));
      }
    }
  }

  private BsonField accumulator(String field, Expression expression) {
    Preconditions.checkArgument(expression instanceof Call, "not a call %s", expression);
    final Call call = (Call) expression;
    final Operator op = call.operator();
    Preconditions.checkArgument(AggregationOperators.isAggregation(op), "not an aggregation operator: %s", op);
    final String name = "$" + naming.get(extractPath(expression));
    if (op == AggregationOperators.AVG) {
      return Accumulators.avg(field, name);
    } else if (op == AggregationOperators.COUNT) {
      return Accumulators.sum(field, 1);
    } else if (op == AggregationOperators.MAX) {
      return Accumulators.max(field, name);
    } else if (op == AggregationOperators.MIN) {
      return Accumulators.min(field, name);
    } else if (op == AggregationOperators.SUM) {
      return Accumulators.sum(field, name);
    } else {
      throw new IllegalArgumentException(String.format("Unknown aggregation operator %s from %s", op, expression));
    }
  }


  private class Sort implements Pipeline {

    @Override
    public void process(Consumer<Bson> consumer) {
      final Function<Collation, Bson> toSortFn = col -> {
        final String name = naming.get(col.path());
        return col.direction().isAscending() ? Sorts.ascending(name) : Sorts.descending(name);

      };

      BsonDocument sort = new BsonDocument();
      for (Collation collation: query.collations()) {
        sort.putAll(toSortFn.apply(collation).toBsonDocument(BsonDocument.class, registry));
      }

      if (!sort.isEmpty()) {
        consumer.accept(Aggregates.sort(sort));
      }
    }
  }

  private class Skip implements Pipeline {
    @Override
    public void process(Consumer<Bson> consumer) {
      query.offset().ifPresent(offset -> consumer.accept(Aggregates.skip((int) offset)));
    }
  }

  private class Limit implements Pipeline {
    @Override
    public void process(Consumer<Bson> consumer) {
      query.limit().ifPresent(limit -> consumer.accept(Aggregates.limit((int) limit)));
    }
  }

}
