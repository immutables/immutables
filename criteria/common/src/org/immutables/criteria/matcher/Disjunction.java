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

package org.immutables.criteria.matcher;

/**
 * Simple {@code OR} statement without arguments. Used to build
 * <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">disjunctive normal form</a> fluent DSL.
 * Contrary to {@link OrMatcher} does not have any arguments.
 *
 * @param <R> root type
 */
public interface Disjunction<R> extends Matcher {

  /**
   * Builds a disjunction with boolean {@code OR}.
   *
   * <p>By default, boolean expressions are combined using
   * <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">disjunctive normal form</a>.
   * @return new root criteria with updated expression
   */
  default R or() {
    return Matchers.extract(this).or().create();
  }
}
