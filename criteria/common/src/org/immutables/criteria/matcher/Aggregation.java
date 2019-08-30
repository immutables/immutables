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

import org.immutables.criteria.expression.AggregationOperators;
import org.immutables.criteria.expression.Expressions;

import java.util.Optional;
import java.util.OptionalDouble;

public interface Aggregation<T> extends Projection<T> {

  interface ObjectAggregation extends Count<Long> {}

  interface ComparableAggregation<T> extends Min<T>, Max<T>, ObjectAggregation {}

  interface NumberAggregation<T> extends ComparableAggregation<T>, Avg<Double>, Sum<Double> {}

  interface OptionalComparableAggregation<T> extends ComparableAggregation<Optional<T>>  {}

  interface NullableComparableAggregation<T> extends ComparableAggregation<T> {}

  interface OptionalNumberAggregation<T> extends OptionalComparableAggregation<T>, Avg<OptionalDouble>, Sum<OptionalDouble> {}

  interface NullableNumberAggregation<T> extends NullableComparableAggregation<T>, Avg<Double>, Sum<Double> {}

  interface Min<T> {
    default Aggregation<T> min() {
      return Matchers.extract((Matcher) this).applyRaw(e -> Expressions.aggregation(AggregationOperators.MIN, Matchers.aggregationType(getClass(), Min.class, "min"), e)).createWith(creator());
    }
  }

  interface Max<T>  {
    default Aggregation<T> max() {
      return Matchers.extract((Matcher) this).applyRaw(e -> Expressions.aggregation(AggregationOperators.MAX, Matchers.aggregationType(getClass(), Max.class, "max"), e)).createWith(creator());
    }
  }

  interface Count<T>  {
    default Aggregation<T> count() {
      return Matchers.extract((Matcher) this).applyRaw(e -> Expressions.aggregation(AggregationOperators.COUNT, Matchers.aggregationType(getClass(), Count.class, "count"), e)).createWith(creator());
    }
  }

  interface Avg<T> {
    default Aggregation<T> avg() {
      return Matchers.extract((Matcher) this).applyRaw(e -> Expressions.aggregation(AggregationOperators.AVG, Matchers.aggregationType(getClass(), Avg.class, "avg"), e)).createWith(creator());
    }
  }

  interface Sum<T> {
    default Aggregation<T> sum() {
      return Matchers.extract((Matcher) this).applyRaw(e -> Expressions.aggregation(AggregationOperators.SUM, Matchers.aggregationType(getClass(), Sum.class, "sum"), e)).createWith(creator());
    }
  }

  @SuppressWarnings("unchecked")
  static <T> CriteriaCreator<T> creator() {
    class Local extends AbstractContextHolder implements Aggregation<T>, Matcher {
      private Local(CriteriaContext context) {
        super(context);
      }
    }

    return ctx -> (T) new Local(ctx);
  }

}
