/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.gson.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ExpectedSubtypesAdapterTest {
  final TypeToken<Long> longType = TypeToken.get(Long.class);
  final TypeToken<Boolean> booleanType = TypeToken.get(Boolean.class);
  final TypeToken<Object> objectType = TypeToken.get(Object.class);
  final Gson gson = new Gson();

  @Test
  public void readSubtype() {
    Gson gson = new Gson();

    ExpectedSubtypesAdapter<Object> subtypesAdaper =
        ExpectedSubtypesAdapter.create(gson, objectType, longType, booleanType);

    check(subtypesAdaper.fromJsonTree(new JsonPrimitive(true))).is(true);
    check(subtypesAdaper.fromJsonTree(new JsonPrimitive(111))).is(111L);
  }

  @Test
  public void writeSubtype() {
    Gson gson = new Gson();

    ExpectedSubtypesAdapter<Object> subtypesAdaper =
        ExpectedSubtypesAdapter.create(gson, objectType, longType, booleanType);

    check(subtypesAdaper.toJsonTree(true)).is(new JsonPrimitive(true));
    check(subtypesAdaper.toJsonTree(111L)).is(new JsonPrimitive(111L));
  }

  @Test(expected = JsonParseException.class)
  public void failSubtype() {

    ExpectedSubtypesAdapter<Object> subtypesAdaper =
        ExpectedSubtypesAdapter.create(gson, objectType, longType, booleanType);

    subtypesAdaper.fromJsonTree(new JsonArray());
  }
}
