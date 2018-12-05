/*
   Copyright 2013-2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.criteria.constraints;


import com.google.common.collect.Range;
import org.immutables.criteria.DocumentCriteria;

/**
 * Criteria for comparables (like {@code >, <=, >} and ranges).
 */
public class ComparableCriteria<V extends Comparable<V>, C extends DocumentCriteria<C, T>, T>
        extends ObjectCriteria<V, C, T> {

  public ComparableCriteria(String name, Constraints.Constraint constraint, CriteriaCreator<C, T> creator) {
    super(name, constraint, creator);
  }

  /**
   * Checks that attribute is in {@code range}.
   */
  public C isIn(Range<V> range) {
    return creator.create(constraint.range(name, false, range));
  }

  /**
   * Checks that attribute is <i>not</i> in {@code range}.
   */
  public C isNotIn(Range<V> range) {
    return creator.create(constraint.range(name, true, range));
  }

  /**
   * Checks that attribute is less than (but not equal to) {@code upper}.
   * <p>Use {@link #isAtMost(Comparable)} for less <i>or equal</i> comparison</p>
   */
  public C isLessThan(V upper) {
    return isIn(Range.lessThan(upper));
  }

  /**
   * Checks that attribute is greater than (but not equal to)  {@code lower}.
   * <p>Use {@link #isAtLeast(Comparable)} for greater <i>or equal</i> comparison</p>
   */
  public C isGreaterThan(V lower) {
    return isIn(Range.greaterThan(lower));
  }

  /**
   * Checks that attribute is less than or equal to {@code upperInclusive}.
   */
  public C isAtMost(V upperInclusive) {
    return isIn(Range.atMost(upperInclusive));
  }

  /**
   * Checks that attribute is greater or equal to {@code lowerInclusive}.
   */
  public C isAtLeast(V lowerInclusive) {
    return isIn(Range.atLeast(lowerInclusive));
  }

}
