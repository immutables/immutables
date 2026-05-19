/*
   Copyright 2024 Immutables Authors and Contributors

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
package org.immutables.fixture;

import org.immutables.fixture.OptionalParameterFixture.SimpleOptional;
import org.immutables.fixture.OptionalParameterFixture.MultipleOptional;
import org.immutables.fixture.OptionalParameterFixture.NullableOptional;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

/**
 * Tests for @Value.Parameter(optional = true) feature which generates
 * telescoping of() factory methods.
 */
public class OptionalParameterTest {

  @Test
  public void simpleOptionalWithAllParameters() {
    SimpleOptional value = ImmutableSimpleOptional.of("John", 30, "Johnny");
    check(value.name()).is("John");
    check(value.age()).is(30);
    check(value.nickname()).is("Johnny");
  }

  @Test
  public void simpleOptionalWithoutOptionalParameter() {
    // This of() overload should be generated, using default for nickname
    SimpleOptional value = ImmutableSimpleOptional.of("John", 30);
    check(value.name()).is("John");
    check(value.age()).is(30);
    check(value.nickname()).is(""); // default value
  }

  @Test
  public void multipleOptionalWithAllParameters() {
    MultipleOptional value = ImmutableMultipleOptional.of("req1", 100, "opt1", "opt2", 99);
    check(value.required1()).is("req1");
    check(value.required2()).is(100);
    check(value.optional1()).is("opt1");
    check(value.optional2()).is("opt2");
    check(value.optional3()).is(99);
  }

  @Test
  public void multipleOptionalWithThreeParameters() {
    // Omit optional3 (last optional)
    MultipleOptional value = ImmutableMultipleOptional.of("req1", 100, "opt1", "opt2");
    check(value.required1()).is("req1");
    check(value.required2()).is(100);
    check(value.optional1()).is("opt1");
    check(value.optional2()).is("opt2");
    check(value.optional3()).is(42); // default value
  }

  @Test
  public void multipleOptionalWithTwoOptionalParameters() {
    // Omit optional2 and optional3
    MultipleOptional value = ImmutableMultipleOptional.of("req1", 100, "opt1");
    check(value.required1()).is("req1");
    check(value.required2()).is(100);
    check(value.optional1()).is("opt1");
    check(value.optional2()).isNull();
    check(value.optional3()).is(42); // default value
  }

  @Test
  public void multipleOptionalWithOnlyRequiredParameters() {
    // Omit all optional parameters
    MultipleOptional value = ImmutableMultipleOptional.of("req1", 100);
    check(value.required1()).is("req1");
    check(value.required2()).is(100);
    check(value.optional1()).is("default1"); // default value
    check(value.optional2()).isNull();
    check(value.optional3()).is(42); // default value
  }

  @Test
  public void nullableOptionalWithAllParameters() {
    NullableOptional value = ImmutableNullableOptional.of("id123", "A description");
    check(value.id()).is("id123");
    check(value.description()).is("A description");
  }

  @Test
  public void nullableOptionalWithoutOptionalParameter() {
    // This of() overload should be generated
    NullableOptional value = ImmutableNullableOptional.of("id123");
    check(value.id()).is("id123");
    check(value.description()).isNull();
  }

  @Test
  public void nullableOptionalWithExplicitNull() {
    // Can still pass null explicitly to the full of() method
    NullableOptional value = ImmutableNullableOptional.of("id123", null);
    check(value.id()).is("id123");
    check(value.description()).isNull();
  }
}
