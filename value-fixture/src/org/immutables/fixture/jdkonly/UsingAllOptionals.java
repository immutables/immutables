/*
    Copyright 2015 Immutables Authors and Contributors

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

import com.atlassian.fugue.Option;
import com.google.common.base.Optional;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface UsingAllOptionals {
  Optional<Integer> v1();

  java.util.Optional<Integer> v2();

  OptionalInt i1();

  OptionalLong l1();

  OptionalDouble d1();

  Option<String> fo2();

  io.atlassian.fugue.Option<String> fo3();

  class Use {
    void use() {
      UsingAllOptionals value =
          ImmutableUsingAllOptionals.builder()
              .v1(1)
              .v2(2)
              .i1(OptionalInt.of(1))
              .d1(1.1)
              .l1(OptionalLong.empty())
              .build();

      Objects.equals(value.v1().get(), value.v2().get());
      Objects.hash(value.i1(), value.l1(), value.d1());
    }
  }
}
