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

import org.immutables.criteria.DocumentCriteria;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Criteria for optional attributes.
 */
public class OptionalCriteria<R extends DocumentCriteria<R>, V, S extends ValueCriteria<R, V>, C extends ValueCriteria<?, V>>  {

  private final CriteriaContext<R> context;

  public OptionalCriteria(CriteriaContext<R> context) {
   this.context = Objects.requireNonNull(context, "context");
  }

  public R isPresent() {
    return context.create(e -> Expressions.call(Operators.IS_PRESENT, e));
  }

  public R isAbsent() {
    return context.create(e -> Expressions.call(Operators.IS_ABSENT, e));
  }

  public S value() {
    throw new UnsupportedOperationException();
  }

  public R value(Consumer<C> consumer) {
    throw new UnsupportedOperationException();
  }

}
