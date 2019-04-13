package org.immutables.criteria.constraints;

import org.immutables.criteria.DocumentCriteria;

public interface Disjunction<R extends DocumentCriteria<R>> {

  /**
   * Builds a disjunction
   */
  R or();
}
