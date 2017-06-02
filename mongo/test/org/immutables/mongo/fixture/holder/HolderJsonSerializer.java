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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * Custom serializer which allows to (JSON) store different types of objects inside same class :
 * {@link Holder}
 */
public class HolderJsonSerializer implements JsonSerializer<Holder>, JsonDeserializer<Holder> {

  private static final String VALUE_PROPERTY = "value";

  @Override
  public Holder deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
    JsonObject root = (JsonObject) json;

    ImmutableHolder.Builder builder = ImmutableHolder.builder();

    if (root.has("id")) {
      builder.id(root.get("id").getAsString());
    }

    JsonElement value = root.get(VALUE_PROPERTY);
    if (value == null) {
      throw new JsonParseException(String.format("%s not found for %s in JSON", VALUE_PROPERTY, type));
    }

    if (value.isJsonObject()) {
      final String valueTypeName = value.getAsJsonObject().get(Holder.TYPE_PROPERTY).getAsString();
      try {
        Class<?> valueType = Class.forName(valueTypeName);
        builder.value(context.deserialize(value, valueType));
      } catch (ClassNotFoundException e) {
        throw new JsonParseException(String.format("Couldn't construct value class %s for %s", valueTypeName, type), e);
      }
    } else if (value.isJsonPrimitive()) {
      final JsonPrimitive primitive = value.getAsJsonPrimitive();
      if (primitive.isString()) {
        builder.value(primitive.getAsString());
      } else if (primitive.isNumber()) {
        builder.value(primitive.getAsInt());
      } else if (primitive.isBoolean()) {
        builder.value(primitive.getAsBoolean());
      }
    } else {
      throw new JsonParseException(
          String.format("Couldn't deserialize %s : %s. Not a primitive or object", VALUE_PROPERTY, value));
    }

    return builder.build();

  }

  @Override
  public JsonElement serialize(Holder src, Type type, JsonSerializationContext context) {
    JsonObject root = new JsonObject();
    JsonElement value = context.serialize(src.value());

    root.addProperty("id", src.id());

    if (value.isJsonObject()) {
      value.getAsJsonObject().addProperty(Holder.TYPE_PROPERTY, src.value().getClass().getName());
    }

    root.add(VALUE_PROPERTY, value);
    return root;
  }

}
