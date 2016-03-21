/*
   Copyright 2016 Immutables Authors and Contributors

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

import java.util.OptionalInt;
import com.google.common.base.Optional;
import org.immutables.builder.Builder;

// Compilation test for usage and mixture of optionals for factory builder
public interface JdkOptionalBuilderFactory {
  @Builder.Factory
  public static int appl(Optional<Integer> a, java.util.Optional<String> b, java.util.OptionalInt c) {
    return a.hashCode() + b.hashCode() + c.hashCode();
  }

  @Builder.Factory
  public static int bbz(@Builder.Parameter java.util.Optional<String> b, @Builder.Parameter java.util.OptionalInt c) {
    return b.hashCode() + c.hashCode();
  }

  static void use() {
    new ApplBuilder()
        .a(1)
        .b("")
        .c(1)
        .build();

    new BbzBuilder(java.util.Optional.empty(), OptionalInt.empty()).build();
  }
}
