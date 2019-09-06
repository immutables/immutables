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

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.backend.UniqueCachedNaming;
import org.immutables.criteria.expression.AggregationCall;
import org.immutables.criteria.expression.AggregationOperators;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helps build aggregate query in elastic
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html">Search Aggregations</a>
 */
class AggregateQueryBuilder {

  /**
   * Used for constructing (possibly nested) Elastic aggregation nodes.
   */
  private static final String AGGREGATIONS = "aggregations";


  private final Query query;
  private final Mapping mapping;
  private final UniqueCachedNaming<Expression> naming;
  private final ObjectMapper mapper;
  private final JsonNodeFactory nodeFactory;

  AggregateQueryBuilder(Query query, ObjectMapper mapper, Mapping mapping) {
    this.query = Objects.requireNonNull(query, "query");
    Preconditions.checkArgument(query.hasAggregations(), "no aggregations for query %s", query);
    this.mapping = mapping;
    List<Expression> toName = new ArrayList<>();
    toName.addAll(query.projections());
    toName.addAll(query.collations().stream().map(Collation::expression).collect(Collectors.toList()));
    toName.addAll(query.groupBy());
    naming = UniqueCachedNaming.of(toName);
    this.mapper = mapper;
    this.nodeFactory = mapper.getNodeFactory();
  }

  ObjectNode jsonQuery() {

    if (!query.groupBy().isEmpty() && query.offset().isPresent()) {
      String message = "Currently ES doesn't support generic pagination "
              + "with aggregations. You can still use LIMIT keyword (without OFFSET). "
              + "For more details see https://github.com/elastic/elasticsearch/issues/4915";
      throw new UnsupportedOperationException(message);
    }

    final ObjectNode json = nodeFactory.objectNode();


    json.put("_source", false);
    json.put("size", 0);

    query.filter().ifPresent(f -> json.set("query", Elasticsearch.query(mapper).convert(f)));

    // due to ES aggregation format. fields in "order by" clause should go first
    // if "order by" is missing. order in "group by" is un-important
    final Set<Expression> orderedGroupBy = new LinkedHashSet<>();
    orderedGroupBy.addAll(query.collations().stream().map(Collation::expression).collect(Collectors.toList()));
    orderedGroupBy.addAll(query.groupBy());

    // construct nested aggregations node(s)
    ObjectNode parent = json.with(AGGREGATIONS);
    for (Expression expr: orderedGroupBy) {
      final String name = ((Path) expr).toStringPath();
      final String aggName = naming.name(expr);
      final ObjectNode section = parent.with(aggName);
      final ObjectNode terms = section.with("terms");
      terms.put("field", name);

      mapping.missingValueFor(name).ifPresent(m -> {
        // expose missing terms. each type has a different missing value
        terms.set("missing", m);
      });

      query.limit().ifPresent(limit ->terms.put("size", limit));

      query.collations().stream()
              .filter(c -> c.path().toStringPath().equals(name))
              .findAny()
              .ifPresent(col -> terms.with("order").put("_key", col.direction().isAscending() ? "asc" : "desc"));

      parent = section.with(AGGREGATIONS);
    }

    for (Expression expr: query.projections()) {
      if (expr instanceof AggregationCall) {
        AggregationCall call = (AggregationCall) expr;
        ObjectNode agg = nodeFactory.objectNode();
        String field = ((Path) call.arguments().get(0)).toStringPath();
        agg.with(toElasticAggregate(call)).put("field", field);
        parent.set(naming.name(call), agg);
      }
    }

    // cleanup json. remove empty "aggregations" element (if empty)
    removeEmptyAggregation(json);
    return json;
  }

  List<ProjectedTuple> processResult(Json.Result result) {

    final List<ProjectedTuple> tuples = new ArrayList<>();
    if (result.aggregations() != null) {
      Converter<String, Expression> converter = naming.asConverter().reverse();
      // collect values
      Json.visitValueNodes(result.aggregations(), m -> {
        Map<Expression, Object> values = Maps.newHashMapWithExpectedSize(query.projections().size());

        for (String field: m.keySet()) {
          Expression expression = converter.convert(field);
          Object value = m.get(field);
          if (value == null) {
            // otherwise jackson returns null even for optionals
            value = NullNode.getInstance();
          } else if (value instanceof Number && (expression.returnType() == LocalDate.class || expression.returnType() == LocalDateTime.class)) {
            // hack/work-around because JavaTimeModule doesn't handle epoch millis for LocalDate and LocalDateTime
            // and elastic always returns epoch millis
            // this ideally should be handled directly by Deserializer
            Instant instant = Instant.ofEpochMilli(((Number) value).longValue());
            value = nodeFactory.textNode(instant.toString());
          }

          values.put(expression, mapper.convertValue(value, mapper.getTypeFactory().constructType(expression.returnType())));
        }

        List<Object> projections = query.projections().stream().map(values::get).collect(Collectors.toList());
        tuples.add(ProjectedTuple.of(query.projections(), projections));
      });
    }

    // elastic exposes total number of documents matching a query in "/hits/total" path
    // this can be used for simple "select count(*) from table"
    final long total = result.searchHits().total().value();

    return tuples;
  }

  private static void removeEmptyAggregation(JsonNode node) {
    if (!node.has(AGGREGATIONS)) {
      node.elements().forEachRemaining(AggregateQueryBuilder::removeEmptyAggregation);
      return;
    }
    JsonNode agg = node.get(AGGREGATIONS);
    if (agg.size() == 0) {
      ((ObjectNode) node).remove(AGGREGATIONS);
    } else {
      removeEmptyAggregation(agg);
    }
  }

  /**
   * Most of the aggregations can be retrieved with single
   * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-stats-aggregation.html">stats</a>
   * function. But currently only one-to-one mapping is supported between sql agg and elastic
   * aggregation.
   */
  private static String toElasticAggregate(AggregationCall call) {
    final AggregationOperators kind = (AggregationOperators) call.operator();
    switch (kind) {
      case COUNT:
        return "value_count";
      case SUM:
        return "sum";
      case MIN:
        return "min";
      case MAX:
        return "max";
      case AVG:
        return "avg";
      default:
        throw new IllegalArgumentException("Unknown aggregation kind " + kind + " for " + call);
    }
  }

}
