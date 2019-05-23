package org.immutables.criteria.elasticsearch;

import org.immutables.criteria.constraints.Expression;

import java.util.Objects;

final class Elasticsearch {

  private  Elasticsearch() {}


  static String toQuery(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    return "{}";
  }

}
