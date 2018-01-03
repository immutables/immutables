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
package org.immutables.fixture.encoding;

import java.util.Date;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.immutables.fixture.encoding.defs.CompactDateEnabled;
import org.immutables.fixture.encoding.defs.CompactOptionalDoubleEnabled;
import org.immutables.fixture.encoding.defs.CompactOptionalIntEnabled;
import org.immutables.fixture.encoding.defs.VoidEncodingEnabled;
import org.immutables.value.Value;

@CompactOptionalIntEnabled
@CompactOptionalDoubleEnabled
@CompactDateEnabled
@Value.Immutable
public abstract class EncodingWithDefaultDerived {
  abstract OptionalInt a();

  abstract Date dt();

  public @Value.Default OptionalInt i() {
    return OptionalInt.empty();
  }

  public @Value.Derived OptionalDouble d() {
    return OptionalDouble.of(0.5);
  }

  public @Value.Default Void v() {
    return null;
  }

  public @Value.Derived Void dv() {
    return null;
  }

  public @Value.Derived Date ddt() {
    return null;
  }
}

@CompactOptionalDoubleEnabled
@CompactOptionalIntEnabled
@VoidEncodingEnabled
@Value.Immutable(singleton = true)
interface DefaultSomeMore {
  OptionalDouble f();

  default @Value.Derived OptionalInt i() {
    return OptionalInt.of(1);
  }

  default @Value.Default OptionalDouble d() {
    return OptionalDouble.empty();
  }
}

@CompactOptionalDoubleEnabled
@CompactOptionalIntEnabled
@Value.Immutable(builder = false)
@Value.Style(privateNoargConstructor = true)
interface DefaultBuilderless {
  @Value.Parameter
  OptionalDouble f();

  default @Value.Derived OptionalInt i() {
    return OptionalInt.of(1);
  }

  default @Value.Default OptionalDouble d() {
    return OptionalDouble.empty();
  }
}
