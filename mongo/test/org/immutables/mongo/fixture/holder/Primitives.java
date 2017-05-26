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
package org.immutables.mongo.fixture.holder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * To test the bug with embedded primitives
 */
@Value.Immutable
@JsonSerialize(as = ImmutablePrimitives.class)
@JsonDeserialize(as = ImmutablePrimitives.class)
@Gson.TypeAdapters
public interface Primitives {

  boolean booleanValue();

  byte byteValue();

  short shortValue();

  int intValue();

  long longValue();

  float floatValue();

  double doubleValue();
}
