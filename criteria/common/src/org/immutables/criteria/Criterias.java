package org.immutables.criteria;

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressional;

import java.util.Objects;

public final class Criterias {

  private Criterias() {}

  /**
   * Extracts {@link Expressional} interface from a criteria. Any criteria implements
   * {@code Expressional} interface at runtime.
   */
  public static Expressional toExpressional(DocumentCriteria<?> criteria) {
    Objects.requireNonNull(criteria, "criteria");
    return (Expressional) criteria;
  }

  /**
   * Extract directly expression from a criteria
   */
  public static Expression toExpression(DocumentCriteria<?> criteria) {
    return toExpressional(criteria).expression();
  }

}
