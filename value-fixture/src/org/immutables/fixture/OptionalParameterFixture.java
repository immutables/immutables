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

import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Test fixture for @Value.Parameter(optional = true) feature.
 * Optional parameters must be at the end and have defaults or be nullable.
 */
public class OptionalParameterFixture {

  /**
   * Simple case: two required parameters, one optional with default.
   */
  @Value.Immutable
  public interface SimpleOptional {
    @Value.Parameter
    String name();

    @Value.Parameter
    int age();

    @Value.Parameter(optional = true)
    @Value.Default
    default String nickname() {
      return "";
    }
  }

  /**
   * Multiple optional parameters at the end - should generate telescoping of() methods.
   */
  @Value.Immutable
  public interface MultipleOptional {
    @Value.Parameter
    String required1();

    @Value.Parameter
    int required2();

    @Value.Parameter(optional = true)
    @Value.Default
    default String optional1() {
      return "default1";
    }

    @Value.Parameter(optional = true)
    @Nullable
    String optional2();

    @Value.Parameter(optional = true)
    @Value.Default
    default int optional3() {
      return 42;
    }
  }

  /**
   * Optional parameter that is nullable (no @Value.Default needed).
   */
  @Value.Immutable
  public interface NullableOptional {
    @Value.Parameter
    String id();

    @Value.Parameter(optional = true)
    @Nullable
    String description();
  }
}
