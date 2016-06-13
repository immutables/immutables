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
package org.immutables.fixture.jackson.poly2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

// Compilation test for unresolved/not-yet-generated class literals inside annotations
// Workaround for Javac bug(peculiarity) of outputting "<error>" string literal instead of class literal
@JsonDeserialize
@Value.Immutable
abstract class _AbstractResponse {
    @JsonProperty("type")
    public abstract String getType();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "one", value = PayloadOne.class),
      @JsonSubTypes.Type(name = "two", value = org.immutables.fixture.jackson.poly2.PayloadTwo.class)
    })
    @JsonProperty("payload")
    public abstract Payload getPayload();

}
