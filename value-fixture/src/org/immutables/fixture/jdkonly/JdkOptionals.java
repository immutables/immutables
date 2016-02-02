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

import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.immutables.gson.Gson;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Serial.Structural
@Value.Immutable(builder = false, singleton = true, intern = true, prehash = true)
@Gson.TypeAdapters
public interface JdkOptionals {
  @Value.Parameter
  Optional<String> v2();

  @Value.Parameter
  OptionalInt i1();

  @Value.Parameter
  OptionalLong l1();

  @Value.Parameter
  OptionalDouble d1();
}

@Value.Immutable
@Value.Style(privateNoargConstructor = true)
interface JdkOptionalsSer extends Serializable {
  @Value.Parameter
  Optional<String> v2();

  @Value.Parameter
  OptionalInt i1();

  @Value.Parameter
  OptionalLong l1();

  @Value.Parameter
  OptionalDouble d1();
}
