/*
   Copyright 2025 Immutables Authors and Contributors

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
package org.immutables.fixture.jdkonly;

import static org.immutables.check.Checkers.check;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

public class JdkOptionalDefaultTest {

  @Test
  public void optionalDefault() {
    check(ImmutableOptionalDefault.builder().build().text()).is(Optional.of("foo"));
    check(ImmutableOptionalDefault.builder().text(Optional.empty()).build().text()).is(Optional.empty());
    check(ImmutableOptionalDefault.builder().text(Optional.of("bar")).build().text()).is(Optional.of("bar"));
    check(ImmutableOptionalDefault.builder().text("bar").build().text()).is(Optional.of("bar"));
  }

  @Test
  public void optionalIntDefault() {
    check(ImmutableOptionalIntDefault.builder().build().magic()).is(OptionalInt.of(42));
    check(ImmutableOptionalIntDefault.builder().magic(OptionalInt.empty()).build().magic()).is(OptionalInt.empty());
    check(ImmutableOptionalIntDefault.builder().magic(OptionalInt.of(17)).build().magic()).is(OptionalInt.of(17));
    check(ImmutableOptionalIntDefault.builder().magic(17).build().magic()).is(OptionalInt.of(17));
  }

  @Test
  public void optionalLongDefault() {
    check(ImmutableOptionalLongDefault.builder().build().magic()).is(OptionalLong.of(42));
    check(ImmutableOptionalLongDefault.builder().magic(OptionalLong.empty()).build().magic()).is(OptionalLong.empty());
    check(ImmutableOptionalLongDefault.builder().magic(OptionalLong.of(17)).build().magic()).is(OptionalLong.of(17));
    check(ImmutableOptionalLongDefault.builder().magic(17).build().magic()).is(OptionalLong.of(17));
  }

  @Test
  public void optionalDoubleDefault() {
    check(ImmutableOptionalDoubleDefault.builder().build().magic()).is(OptionalDouble.of(42));
    check(ImmutableOptionalDoubleDefault.builder().magic(OptionalDouble.empty()).build().magic()).is(OptionalDouble.empty());
    check(ImmutableOptionalDoubleDefault.builder().magic(OptionalDouble.of(17)).build().magic()).is(OptionalDouble.of(17));
    check(ImmutableOptionalDoubleDefault.builder().magic(17).build().magic()).is(OptionalDouble.of(17));
  }
}
