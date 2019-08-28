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

import static org.immutables.check.Checkers.check;

public class MatchersTest {

  private static class Dummy implements Aggregation.Count<Long>, Aggregation.Avg<OptionalDouble>, Aggregation.Min<Optional<Double>>, Aggregation.Sum<Integer> {}

  @Test
  public void genericType() {
    Dummy dummy = new Dummy();
    check(Matchers.genericTypeOf(dummy, Aggregation.Count.class)).is(Long.class);
    check(Matchers.genericTypeOf(dummy, Aggregation.Avg.class)).is(OptionalDouble.class);
    check(Matchers.genericTypeOf(dummy, Aggregation.Min.class)).is(new TypeToken<Optional<Double>>() {}.getType());
    check(Matchers.genericTypeOf(dummy, Aggregation.Sum.class)).is(Integer.class);
  }
}