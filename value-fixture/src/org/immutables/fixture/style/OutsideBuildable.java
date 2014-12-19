/*
    Copyright 2014 Immutables Authors and Contributors

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

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable.ImplementationVisibility;

/**
 * Feature combination
 * <ul>
 * <li>Builder outside generated with disambiguation pattern.
 * <li>uses factory method.
 * </ul>
 */
@Value.Immutable(visibility = ImplementationVisibility.PRIVATE)
public class OutsideBuildable {

  void use() {
    OutsideBuildableBuilder.builder().build();
  }
}

/**
 * Feature combination
 * <ul>
 * <li>Builder outside generated with disambiguation pattern.
 * <li>uses factory method.
 * </ul>
 */
@Value.Immutable(visibility = ImplementationVisibility.PRIVATE)
@Value.Style(builder = "new")
class OutsideBuildableNew {

  void use() {
    new OutsideBuildableNewBuilder().build();
  }
}
