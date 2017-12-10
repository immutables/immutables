/*
   Copyright 2017 Immutables Authors and Contributors

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

import com.google.common.base.Optional;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(allParameters = true)
public interface OptionalCast {
  Optional<String> getBar();

  Optional<Object> getObject();

  Optional<String[]> getStringArray();

  Optional<List<Long>> getList();

  @SuppressWarnings("CheckReturnValue")
  default void use() {
    ImmutableOptionalCast.of(
        Optional.absent(),
        Optional.of("String is object"),
        Optional.<String[]>absent(),
        Optional.absent());
  }
}
