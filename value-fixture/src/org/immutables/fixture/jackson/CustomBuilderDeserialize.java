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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCustomBuilderDeserialize.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
// the annotation will be copied to builder
public abstract class CustomBuilderDeserialize {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  @JsonSubTypes({
      @Type(name = "I", value = Integer.class),
      @Type(name = "O", value = Double.class)
  })
  // the annotation will be copied to a builder setter
  public abstract @Nullable Object value();

  public abstract int a();

  public abstract @Nullable String s();

  public abstract List<Boolean> l();
}
