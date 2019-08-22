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

import java.util.Optional;
import java.util.OptionalDouble;

public interface Aggregation<T> extends Projection<T> {

  interface ObjectAggregation extends Count<Long> {}

  interface ComparableAggregation<T> extends Min<T>, Max<T> {}

  interface NumberAggregation<T> extends ComparableAggregation<T>, ObjectAggregation, Avg<Double>, Sum<Double> {}

  interface OptionalComparableAggregation<T> extends ComparableAggregation<Optional<T>>, ObjectAggregation {}

  interface NullableComparableAggregation<T> extends ComparableAggregation<T>, ObjectAggregation {}

  interface OptionalNumberAggregation<T> extends OptionalComparableAggregation<Optional<T>>, Avg<OptionalDouble>, Sum<OptionalDouble> {}

  interface NullableNumberAggregation<T> extends NullableComparableAggregation<T>, Avg<Double>, Sum<Double> {}

  interface Min<T>  {
    default Aggregation<T> min() {
      return null;
    }
  }

  interface Max<T>  {
    default Aggregation<T> max() {
      return null;
    }
  }

  interface Count<T>  {
    default Aggregation<T> count() {
      return null;
    }
  }

  interface Avg<T> {
    default Aggregation<T> avg() {
      return null;
    }
  }

  interface Sum<T> {
    default Aggregation<T> sum() {
      return null;
    }
  }

}
