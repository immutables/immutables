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
package org.immutables.fixture.encoding;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import org.immutables.fixture.encoding.defs.OptionalList2Enabled;

@Value.Style(depluralize = true)
@OptionalList2Enabled
@Value.Immutable
public interface UseOptionalCollections2<V> {

  @Value.Parameter
  Optional<List<String>> as();

  @Value.Parameter
  Optional<List<V>> bs();

  // just to generate builder constructor
  @Value.Default
  default boolean def() {
    return true;
  }
}
