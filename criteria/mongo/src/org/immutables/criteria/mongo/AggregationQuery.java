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
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.criteria.backend.ExpressionNaming;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.AggregationOperators;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Expression;
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
    this.query = Objects.requireNonNull(query, "query");
    this.pathNaming = Objects.requireNonNull(pathNaming, "naming");

    BiMap<Expression, String> biMap = HashBiMap.create();

    List<Path> paths = Stream.concat(query.projections().stream(), Stream.concat(query.groupBy().stream(), query.collations().stream().map(Collation::expression)))
            .map(AggregationQuery::extractPath).collect(Collectors.toList());

    ExpressionNaming naming = ExpressionNaming.of(UniqueCachedNaming.of(paths.iterator()));
    paths.forEach(p -> biMap.put(p, naming.name(p)));

    this.projectionNaming = ExpressionNaming.of(UniqueCachedNaming.of(query.projections()));
    this.naming = ImmutableBiMap.copyOf(biMap);
    this.registry = MongoClientSettings.getDefaultCodecRegistry();
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
    List<Pipeline> pipelines = Arrays.asList(new MatchPipeline(), new NameAndExtractFields(), new Group(), new Sort(), new Skip(), new Limit());
    pipelines.forEach(p -> p.process(aggregates::add));

    return Collections.unmodifiableList(aggregates);
  }

  private interface Pipeline {
    void process(Consumer<Bson> consumer);
  }

  private class MatchPipeline implements Pipeline {
    @Override
    public void process(Consumer<Bson> consumer) {
      query.filter().ifPresent(expr -> consumer.accept(Aggregates.match(expr.accept(new FindVisitor(pathNaming)))));
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
      if (!query.groupBy().isEmpty()) {
        query.groupBy().forEach(groupBy -> {
          String alias = naming.get(extractPath(groupBy));
          id.put(alias, new BsonString("$" + alias));
        });
      }

      // $project part
      if (!query.projections().isEmpty()) {
        for (int i = 0; i < query.projections().size(); i++) {
          Expression expr = query.projections().get(i);
          final String alias = naming.get(extractPath(expr));
          final String uniq = projectionNaming.name(expr);
          if (query.groupBy().contains(expr)) {
            project.put(alias, new BsonString(id.size() == 1 ? "$_id" : "$_id." + alias));
          } else if (expr instanceof Call) {
            // aggregation sum / count etc.
            accumulators.add(accumulator(uniq, expr));
            project.put(uniq, new BsonString("$" + uniq));
          } else {
            // simple projection
            project.put(uniq, new BsonString("$" + uniq));
          }
        }
      }

      if (!accumulators.isEmpty()) {
        consumer.accept(Aggregates.group(id.size() == 1 ? id.values().iterator().next() : id, accumulators));
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
