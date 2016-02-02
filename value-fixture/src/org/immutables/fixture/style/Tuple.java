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
package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Style(
    typeImmutable = "*Tuple",
    allParameters = true,
    defaults = @Value.Immutable(builder = false))
public @interface Tuple {}

@Tuple
@Value.Immutable
interface Color {
  int red();
  int green();
  int blue();
  
  default void use() {
    ColorTuple.of(0xFF, 0x00, 0xFE);
  }
}

@Tuple
@Value.Immutable
interface OverrideColor {
  @Value.Parameter
  int white();

  @Value.Parameter
  int black();

  @Value.Default
  default int gray() {
    return black() - gray();
  }

  default void use() {
    OverrideColorTuple.of(0xFF, 0x00);
  }
}