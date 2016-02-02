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

import java.util.Set;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

/**
 * Feature combination
 * <ul>
 * <li>Abstract type and accessor name detection
 * <li>Generated builder naming customization
 * </ul>
 */
@Value.Immutable(copy = true)
@Value.Style(
    visibility = ImplementationVisibility.PUBLIC,
    get = {"extract*", "collect*"},
    typeAbstract = "Abstract*",
    typeImmutable = "*",
    build = "build*",
    init = "using*",
    add = "with*Appended",
    builder = "newBuilder")
abstract class AbstractValueNamingDetected {

  abstract int extractVal();

  abstract Set<String> collectStr();

  void use() {
    ValueNamingDetected.newBuilder()
        .usingVal(1)
        .withStrAppended("Appended!")
        .buildValueNamingDetected();
  }
}
