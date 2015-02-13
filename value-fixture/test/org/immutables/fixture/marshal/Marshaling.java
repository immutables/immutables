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

  @SuppressWarnings({"resource", "unchecked"})
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
      throw Throwables.propagate(ex);
    }
  }

  @SuppressWarnings({"resource", "unchecked"})
  public static <T> T fromJson(String string, Class<T> type) {
    TypeAdapter<Object> adapter = GSON.getAdapter((TypeToken<Object>) TypeToken.get(type));
    try {
      if (adapter instanceof com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.Adapter) {
        throw new IllegalStateException("Immutable adapters not registered");
      }
      JsonParserReader reader = new JsonParserReader(JSON_FACTORY.createParser(string));
      return GSON.fromJson(reader, type);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }
}
