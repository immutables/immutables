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
package org.immutables.fixture.marshal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ServiceLoader;
import org.immutables.gson.stream.JsonGeneratorWriter;
import org.immutables.gson.stream.JsonParserReader;

public final class Marshaling {
  private Marshaling() {}

  private static final JsonFactory JSON_FACTORY = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
      .disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

  private static final Gson GSON;
  static {
    GsonBuilder gsonBuilder = new GsonBuilder();
    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gsonBuilder.registerTypeAdapterFactory(factory);
    }
    GSON = gsonBuilder.create();
  }

  public static Gson getGson() {
    return GSON;
  }

  @SuppressWarnings({"unchecked"})
  public static String toJson(Object object) {
    TypeAdapter<Object> adapter = GSON.getAdapter((TypeToken<Object>) TypeToken.get(object.getClass()));
    try {
      StringWriter stringWriter = new StringWriter();
      JsonGeneratorWriter writer = new JsonGeneratorWriter(JSON_FACTORY.createGenerator(stringWriter));
      if (adapter instanceof com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.Adapter) {
        throw new IllegalStateException("Immutable adapters not registered");
      }
      GSON.toJson(object, object.getClass(), writer);
      writer.close();
      return stringWriter.toString();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T fromJson(String string, Class<T> type) {
    TypeAdapter<Object> adapter = GSON.getAdapter((TypeToken<Object>) TypeToken.get(type));
    try {
      if (adapter instanceof com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.Adapter) {
        throw new IllegalStateException("Immutable adapters not registered");
      }
      JsonParserReader reader = new JsonParserReader(JSON_FACTORY.createParser(string));
      return GSON.fromJson(reader, type);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
