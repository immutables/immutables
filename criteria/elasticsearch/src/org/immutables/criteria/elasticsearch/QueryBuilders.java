/*
 * Copyright 2019 Immutables Authors and Contributors
 * Copyright 2016-2018 Apache Software Foundation (ASF)
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class to generate elastic search queries. Most query builders have
 * been copied from ES distribution. The reason we have separate definition is
 * high-level client dependency on core modules (like lucene, netty, XContent etc.) which
 * is not compatible between different major versions.
 *
 * <p>The goal of ES adapter is to
 * be compatible with any elastic version or even to connect to clusters with different
 * versions simultaneously.
 *
 * <p>Jackson API is used to generate ES query as JSON document.
 * <p>Some parts of this class have been copied from <a href="https://calcite.apache.org/">Apache Calcite</a> project.
 */
class QueryBuilders {

  private QueryBuilders() {}

  /**
   * A Query that matches documents containing a term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, String value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A Query that matches documents containing a term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, int value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A Query that matches documents containing a single character term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, char value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A Query that matches documents containing a term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, long value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A Query that matches documents containing a term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, float value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A Query that matches documents containing a term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, double value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A Query that matches documents containing a term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, boolean value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A Query that matches documents containing a term.
   *
   * @param name  The name of the field
   * @param value The value of the term
   */
  static TermQueryBuilder termQuery(String name, Object value) {
    return new TermQueryBuilder(name, value);
  }

  /**
   * A filer for a field based on several terms matching on any of them.
   *
   * @param name   The field name
   * @param values The terms
   */
  static TermsQueryBuilder termsQuery(String name, Iterable<?> values) {
    return new TermsQueryBuilder(name, values);
  }

  /**
   * A Query that matches documents within an range of terms.
   *
   * @param name The field name
   */
  static RangeQueryBuilder rangeQuery(String name) {
    return new RangeQueryBuilder(name);
  }

  /**
   * A Query that matches documents containing terms with a specified regular expression.
   *
   * @param name   The name of the field
   * @param regexp The regular expression
   */
  static RegexpQueryBuilder regexpQuery(String name, String regexp) {
    return new RegexpQueryBuilder(name, regexp);
  }


  /**
   * A Query that matches documents matching boolean combinations of other queries.
   */
  static BoolQueryBuilder boolQuery() {
    return new BoolQueryBuilder();
  }

  /**
   * A query that wraps another query and simply returns a constant score equal to the
   * query boost for every document in the query.
   *
   * @param queryBuilder The query to wrap in a constant score query
   */
  static ConstantScoreQueryBuilder constantScoreQuery(QueryBuilder queryBuilder) {
    return new ConstantScoreQueryBuilder(queryBuilder);
  }

  /**
   * A filter to filter only documents where a field exists in them.
   *
   * @param name The name of the field
   */
  static ExistsQueryBuilder existsQuery(String name) {
    return new ExistsQueryBuilder(name);
  }


  static PrefixQueryBuilder prefixQuery(String name, String prefix) {
    return new PrefixQueryBuilder(name, prefix);
  }

  static WildcardQueryBuilder wildcardQuery(String name, String wildcard) {
    return new WildcardQueryBuilder(name, wildcard);
  }

  /**
   * A query that matches on all documents.
   */
  static MatchAllQueryBuilder matchAll() {
    return new MatchAllQueryBuilder();
  }

  /**
   * Base class to build ES queries
   */
  abstract static class QueryBuilder {

    /**
     * Convert existing query to JSON format using jackson API.
     */
    abstract ObjectNode toJson(ObjectMapper mapper);
  }

  /**
   * Query for boolean logic
   */
  static class BoolQueryBuilder extends QueryBuilder {
    private final List<QueryBuilder> mustClauses = new ArrayList<>();
    private final List<QueryBuilder> mustNotClauses = new ArrayList<>();
    private final List<QueryBuilder> filterClauses = new ArrayList<>();
    private final List<QueryBuilder> shouldClauses = new ArrayList<>();

    BoolQueryBuilder must(QueryBuilder queryBuilder) {
      Objects.requireNonNull(queryBuilder, "queryBuilder");
      mustClauses.add(queryBuilder);
      return this;
    }

    BoolQueryBuilder filter(QueryBuilder queryBuilder) {
      Objects.requireNonNull(queryBuilder, "queryBuilder");
      filterClauses.add(queryBuilder);
      return this;
    }

    BoolQueryBuilder mustNot(QueryBuilder queryBuilder) {
      Objects.requireNonNull(queryBuilder, "queryBuilder");
      mustNotClauses.add(queryBuilder);
      return this;
    }

    BoolQueryBuilder should(QueryBuilder queryBuilder) {
      Objects.requireNonNull(queryBuilder, "queryBuilder");
      shouldClauses.add(queryBuilder);
      return this;
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode result = mapper.createObjectNode();
      ObjectNode bool = result.with("bool");
      writeJsonArray("must", mustClauses, bool, mapper);
      writeJsonArray("filter", filterClauses, bool, mapper);
      writeJsonArray("must_not", mustNotClauses, bool, mapper);
      writeJsonArray("should", shouldClauses, bool, mapper);
      return result;
    }

    private static void writeJsonArray(String field, List<QueryBuilder> clauses, ObjectNode node, ObjectMapper mapper) {
      if (clauses.isEmpty()) {
        return;
      }

      if (clauses.size() == 1) {
        node.set(field, clauses.get(0).toJson(mapper));
      } else {
        final ArrayNode arrayNode = node.withArray(field);
        for (QueryBuilder clause: clauses) {
          arrayNode.add(clause.toJson(mapper));
        }
      }
    }
  }

  /**
   * A Query that matches documents containing a term.
   */
  static class TermQueryBuilder extends QueryBuilder {
    private final String fieldName;
    private final Object value;

    private TermQueryBuilder(final String fieldName, final Object value) {
      this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
      this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode result = mapper.createObjectNode();
      result.with("term").set(fieldName, toJsonValue(value, mapper));
      return result;
    }
  }

  /**
   * A filter for a field based on several terms matching on any of them.
   */
  private static class TermsQueryBuilder extends QueryBuilder {
    private final String fieldName;
    private final Iterable<?> values;

    private TermsQueryBuilder(final String fieldName, final Iterable<?> values) {
      this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
      this.values = Objects.requireNonNull(values, "values");
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper)  {
      ObjectNode result = mapper.createObjectNode();
      final ArrayNode terms = result.with("terms").withArray(fieldName);
      for (Object value: values) {
        terms.add(toJsonValue(value, mapper));
      }
      return result;
    }
  }

  /**
   * A Query that matches documents within an range of terms.
   */
  static class RangeQueryBuilder extends QueryBuilder {
    private final String fieldName;

    private Object lt;
    private boolean lte;
    private Object gt;
    private boolean gte;

    private String format;

    private RangeQueryBuilder(final String fieldName) {
      this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
    }

    private RangeQueryBuilder to(Object value, boolean lte) {
      this.lt = Objects.requireNonNull(value, "value");
      this.lte = lte;
      return this;
    }

    private RangeQueryBuilder from(Object value, boolean gte) {
      this.gt = Objects.requireNonNull(value, "value");
      this.gte = gte;
      return this;
    }

    RangeQueryBuilder lt(Object value) {
      return to(value, false);
    }

    RangeQueryBuilder lte(Object value) {
      return to(value, true);
    }

    RangeQueryBuilder gt(Object value) {
      return from(value, false);
    }

    RangeQueryBuilder gte(Object value) {
      return from(value, true);
    }

    RangeQueryBuilder format(String format) {
      this.format = format;
      return this;
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      if (lt == null && gt == null) {
        throw new IllegalStateException("Either lower or upper bound should be provided");
      }

      final ObjectNode result = mapper.createObjectNode();
      final ObjectNode range = result.with("range").with(fieldName);
      if (gt != null) {
        final String op = gte ? "gte" : "gt";
        range.set(op, toJsonValue(gt, mapper));
      }

      if (lt != null) {
        final String op = lte ? "lte" : "lt";
        range.set(op, toJsonValue(lt, mapper));
      }

      if (format != null) {
        range.put("format", format);
      }

      return result;
    }
  }

  /**
   * A Query that does fuzzy matching for a specific value.
   */
  static class RegexpQueryBuilder extends QueryBuilder {
    private final String fieldName;
    private final String value;

    RegexpQueryBuilder(final String fieldName, final String value) {
      this.fieldName = fieldName;
      this.value = value;
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode result = mapper.createObjectNode();
      result.with("regexp").with(fieldName).put("value", value);
      return result;
    }
  }

  /**
   * Constructs a query that only match on documents that the field has a value in them.
   */
  static class ExistsQueryBuilder extends QueryBuilder {
    private final String fieldName;

    ExistsQueryBuilder(final String fieldName) {
      this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode result = mapper.createObjectNode();
      result.with("exists").put("field", fieldName);
      return result;
    }
  }

  /**
   * Constructs a query that only match on documents that the field has a value in them.
   */
  static class PrefixQueryBuilder extends QueryBuilder {
    private final String fieldName;
    private final String value;

    PrefixQueryBuilder(final String fieldName, String value) {
      this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
      this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode result = mapper.createObjectNode();
      result.with("prefix").put(fieldName, value);
      return result;
    }
  }

  static class WildcardQueryBuilder extends QueryBuilder {
    private final String fieldName;
    private final String wildcard;

    private WildcardQueryBuilder(String fieldName, String wildcard) {
      this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
      this.wildcard = Objects.requireNonNull(wildcard, "wildcard");
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode result = mapper.createObjectNode();
      result.with("wildcard").put(fieldName, wildcard);
      return result;
    }
  }

  /**
   * A query that wraps a filter and simply returns a constant score equal to the
   * query boost for every document in the filter.
   */
  static class ConstantScoreQueryBuilder extends QueryBuilder {

    private final QueryBuilder builder;

    private ConstantScoreQueryBuilder(final QueryBuilder builder) {
      this.builder = Objects.requireNonNull(builder, "builder");
    }

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode result = mapper.createObjectNode();
      result.with("constant_score").set("filter", builder.toJson(mapper));
      return result;
    }
  }

  /**
   * A query that matches on all documents.
   * <pre>
   *   {
   *     "match_all": {}
   *   }
   * </pre>
   */
  static class MatchAllQueryBuilder extends QueryBuilder {

    private MatchAllQueryBuilder() {}

    @Override
    ObjectNode toJson(ObjectMapper mapper) {
      ObjectNode node = mapper.createObjectNode();
      node.putObject("match_all");
      return node;
    }
  }

  /**
   * Write usually simple (scalar) value (string, number, boolean or null) to json output.
   * In case of complex objects delegates to jackson serialization.
   *
   * @param value JSON value to write
   */
  private static JsonNode toJsonValue(Object value, ObjectMapper mapper) {
    if (value == null) {
      return mapper.getNodeFactory().nullNode();
    }

    return mapper.convertValue(value, JsonNode.class);
  }
}

