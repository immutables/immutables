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

import com.google.common.collect.ImmutableMap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class GsonFeaturesTest {
  final Gson gsonWithOptions = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .serializeNulls()
      .registerTypeAdapterFactory(new GsonAdaptersSimple())
      .registerTypeAdapterFactory(new GsonAdaptersUnsimple())
      .create();

  final Gson gsonDefault = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersSimple())
      .registerTypeAdapterFactory(new GsonAdaptersUnsimple())
      .registerTypeAdapterFactory(new GsonAdaptersNullAsDefault())
      .registerTypeAdapterFactory(new GsonAdaptersDeserializeEmptyNullable())
      .registerTypeAdapterFactory(new GsonAdaptersNullableArray())
      .create();

  @Test
  public void namingApplied() {
    Simple s1 = ImmutableSimple.builder()
        .addCharacterList('a')
        .optionalString("s")
        .nlb(1)
        .build();

    JsonObject json = gsonWithOptions.toJsonTree(s1).getAsJsonObject();
    check(keysIn(json)).hasAll("character_list", "optional_string", "_nullable_");
  }

  @Test
  public void emptyAsNullsAndSerializeNulls() {
    Simple simple = ImmutableSimple.builder().build();
    JsonObject json = gsonWithOptions.toJsonTree(simple).getAsJsonObject();
    check(keysIn(json)).hasContentInAnyOrder("character_list", "optional_string", "_nullable_");
  }

  @Test
  public void nullAsDefault() {
    NullAsDefault.Val val = gsonDefault.fromJson("{\"a\":null,\"b\":null}", NullAsDefault.Val.class);
    check(val.a()).is(-1);
    check(val.b()).is("n/a");
  }

  @Test
  public void deserializeEmptyNullableCollections() {
    DeserializeEmptyNullable o =
        gsonDefault.fromJson("{\"tags\":[],\"mm\":{},\"mp\":{},\"st\":[]}", DeserializeEmptyNullable.class);
    check(o.tags()).isEmpty();
    check(o.mm().entries()).isEmpty();
    check(o.mp().entrySet()).isEmpty();
    check(o.st()).isEmpty();
  }

  @Test
  public void deserializeNullOrAbsentNullableCollections() {
    DeserializeEmptyNullable o =
        gsonDefault.fromJson("{\"tags\":null,\"mm\":null,\"mp\":null,\"st\":null}", DeserializeEmptyNullable.class);
    check(o.tags()).isNull();
    check(o.mm()).isNull();
    check(o.mp()).isNull();
    check(o.st()).isNull();

    DeserializeEmptyNullable o2 =
        gsonDefault.fromJson("{}", DeserializeEmptyNullable.class);
    check(o2.tags()).isNull();
    check(o2.mm()).isNull();
    check(o2.mp()).isNull();
    check(o2.st()).isNull();
  }

  @Test
  public void emptyAsNullsAndNotSerializeNulls() {
    Simple s1 = ImmutableSimple.builder().build();
    JsonObject json = gsonDefault.toJsonTree(s1).getAsJsonObject();
    check(keysIn(json)).isEmpty();
  }

  @Test
  public void emptyAsNotNulls() {
    Unsimple unsimple = ImmutableUnsimple.builder().build();
    JsonObject json = gsonDefault.toJsonTree(unsimple).getAsJsonObject();
    check(keysIn(json)).isOf("characterList");
  }

  @Test
  public void nullableContainersNames() {
    NullableArray a = ImmutableNullableArray.builder()
        .array("_")
        .addList("_")
        .putMap(0, "_")
        .build();

    String json = gsonDefault.toJson(a);
    check(json).contains("\"a\":");
    check(json).contains("\"l\":");
    check(json).contains("\"m\":");
  }

  private Set<String> keysIn(JsonObject json) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
      builder.put(entry);
    }
    return builder.build().keySet();
  }
}
