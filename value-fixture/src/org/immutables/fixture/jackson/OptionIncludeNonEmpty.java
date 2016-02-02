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
package org.immutables.fixture.jackson;

import javax.annotation.Nullable;
import java.util.Map;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.immutables.value.Value;

interface Inherited {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Nullable
  Integer getInteger();

  OptionIncludeNonEmpty withRelationships(Optional<String> number);
}

@Value.Immutable
@JsonDeserialize(as = ImmutableOptionIncludeNonEmpty.class)
public abstract class OptionIncludeNonEmpty implements Inherited {
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Optional<String> getRelationships();

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Map<String, Number> getMap();

  // used to test abstract method generation in Json class
  public abstract OptionIncludeNonEmpty withMap(Map<String, ? extends Number> number);
}

@Value.Immutable(builder = false)
@JsonDeserialize(as = ImmutableOptionIncludeNonEmptyWithConstructor.class)
abstract class OptionIncludeNonEmptyWithConstructor {

  @Value.Parameter
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public abstract Optional<String> getRelationships();
}