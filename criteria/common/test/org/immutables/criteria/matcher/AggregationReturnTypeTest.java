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

import com.google.common.reflect.TypeToken;
import org.junit.Test;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

import static org.immutables.check.Checkers.check;

/**
 * Validation of generic type variable (at runtime) returned from aggregation types.
 * These types are used during deserialization for aggregated query result: {@code select avg(age) from users}
 */
public class AggregationReturnTypeTest {

  @Test
  public void basic() {
    class Dummy implements Aggregation.Count, Aggregation.Avg<OptionalDouble>, Aggregation.Min<Optional<Double>>, Aggregation.Sum<Integer> {}

    check(Matchers.aggregationType(Dummy.class, Aggregation.Count.class, "count")).is(Long.class);
    check(Matchers.aggregationType(Dummy.class, Aggregation.Avg.class, "avg")).is(OptionalDouble.class);
    check(Matchers.aggregationType(Dummy.class, Aggregation.Min.class, "min")).is(new TypeToken<Optional<Double>>() {}.getType());
    check(Matchers.aggregationType(Dummy.class, Aggregation.Sum.class, "sum")).is(Integer.class);
  }

  @Test
  public void number() {
    class MyNumber implements NumberMatcher.Template<Void, Long> {}
    check(Matchers.aggregationType(MyNumber.class, Aggregation.Max.class, "max")).is(Long.class);
    check(Matchers.aggregationType(MyNumber.class, Aggregation.Min.class, "min")).is(Long.class);
    check(Matchers.aggregationType(MyNumber.class, Aggregation.Sum.class, "sum")).is(Double.class);
    check(Matchers.aggregationType(MyNumber.class, Aggregation.Avg.class, "avg")).is(Double.class);
    check(Matchers.aggregationType(MyNumber.class, Aggregation.Count.class, "count")).is(Long.class);
  }

  @Test
  public void optionalNumber() {
    class MyOptionalNumber implements OptionalNumberMatcher.Template<Void, Long> {}
    check(Matchers.aggregationType(MyOptionalNumber.class, Aggregation.Min.class, "min")).is(new TypeToken<Optional<Long>>() {}.getType());
    check(Matchers.aggregationType(MyOptionalNumber.class, Aggregation.Max.class, "max")).is(new TypeToken<Optional<Long>>() {}.getType());
    check(Matchers.aggregationType(MyOptionalNumber.class, Aggregation.Sum.class, "sum")).is(OptionalDouble.class);
    check(Matchers.aggregationType(MyOptionalNumber.class, Aggregation.Avg.class, "avg")).is(OptionalDouble.class);
    check(Matchers.aggregationType(MyOptionalNumber.class, Aggregation.Count.class, "count")).is(Long.class);
  }

  @Test
  public void comparable() {
    class MyTemplate implements ComparableMatcher.Template<Void, String> {}

    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Min.class, "min")).is(String.class);
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Max.class, "max")).is(String.class);
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Count.class, "count")).is(Long.class);
  }

  @Test
  public void optionalComparable() {
    class MyTemplate implements OptionalComparableMatcher.Template<Void, UUID> {}

    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Min.class, "min")).is(new TypeToken<Optional<UUID>>() {}.getType());
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Max.class, "max")).is(new TypeToken<Optional<UUID>>() {}.getType());
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Count.class, "count")).is(Long.class);
  }

  @Test
  public void object() {
    class MyTemplate implements ObjectMatcher.Template<Void, String> {}
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Count.class, "count")).is(Long.class);
  }

  @Test
  public void booleanMatcher() {
    class MyTemplate implements BooleanMatcher.Template {}
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Count.class, "count")).is(Long.class);
  }

  @Test
  public void string() {
    class MyTemplate implements StringMatcher.Template<Void> {}

    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Min.class, "min")).is(String.class);
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Max.class, "max")).is(String.class);
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Count.class, "count")).is(Long.class);
  }

  @Test
  public void optionalString() {
    class MyTemplate implements OptionalStringMatcher.Template<Void> {}

    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Min.class, "min")).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Max.class, "max")).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.aggregationType(MyTemplate.class, Aggregation.Count.class, "count")).is(Long.class);
  }
}