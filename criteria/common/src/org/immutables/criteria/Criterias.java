package org.immutables.criteria;

import org.immutables.criteria.expression.Expressional;

import java.util.Objects;

public final class Criterias {

  private Criterias() {}

  /**
   * Extracts {@link Expressional} interface from a criteria
   */
  public static Expressional toExpressional(DocumentCriteria<?> criteria) {
    Objects.requireNonNull(criteria, "criteria");
    return (Expressional) criteria;
  }

}
