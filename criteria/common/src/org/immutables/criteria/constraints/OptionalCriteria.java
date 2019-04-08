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

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Criteria for optional attributes.
 */
// TODO do we really need to extend from ObjectCriteria ? perhaps just make ValueCriteria ?
public class OptionalCriteria<R extends DocumentCriteria<R>, V, S extends ValueCriteria<R, V>> extends ObjectCriteria<R, Optional<V>> {

  public OptionalCriteria(CriteriaContext<R> context) {
    super(context);
  }

  public R isPresent() {
    return create(e -> Expressions.call(Operators.IS_PRESENT, e));
  }

  public R isAbsent() {
    return create(e -> Expressions.call(Operators.IS_ABSENT, e));
  }

  public S value() {
    throw new UnsupportedOperationException();
  }

  public R value(Consumer<S> consumer) {
    throw new UnsupportedOperationException();
  }

}
