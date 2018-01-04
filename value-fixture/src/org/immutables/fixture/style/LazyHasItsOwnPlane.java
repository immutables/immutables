/*
   Copyright 2018 Immutables Authors and Contributors

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
package org.immutables.fixture.style;

import java.util.List;
import org.immutables.value.Value;

@Value.Style(get = {"get*", "internal*"})
@Value.Immutable
public abstract class LazyHasItsOwnPlane {
  // use hidden basic collection property to generate useful 'with' and builder methods
  abstract List<String> internalStrings();

  // but expose a collection wrapper that's nicer to use
  @Value.Lazy
  public Object[] getStrings() {
    return internalStrings().toArray();
  }

  // non-overlapping lazy, should not have suffix on field
  @Value.Lazy
  public int getLength() {
    return getStrings().length;
  }
}
