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
package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableJsonValueCreator.class)
public interface JsonValueCreator {
  @JsonValue
  String value();

  @Value.Immutable(singleton = true, builder = false)
  @JsonDeserialize(as = ImmutableJsonValueCreator.Singleton.class)
  public interface Singleton {
    @JsonValue
    @Value.Default
    default double value() {
      return 0.1;
    }
  }

  @Value.Immutable(builder = false)
  @JsonDeserialize(as = ImmutableJsonValueCreator.Constructor.class)
  public interface Constructor {
    @JsonValue
    @Value.Parameter
    boolean value();
  }
}
