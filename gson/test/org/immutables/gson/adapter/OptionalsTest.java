/*
   Copyright 2018 Immutables Authors and Contributors

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

import java.lang.reflect.Type;
import java.time.Instant;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class OptionalsTest {

  final Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonAdaptersOptionals())
      .create();

  Optionals.Host createEmptyHost() {
    return ImmutableOptionals.Host.builder().build();
  }

  Optionals.Host createHost() {
    return ImmutableOptionals.Host.builder()
        .optInt(3)
        .optDouble(3.14D)
        .optWrappedFloat(3.14F)
        .optWrappedInteger(3)
        .optWrappedLong(33L)
        .optWrappedDouble(3.14D)
        .aPoly(ImmutableOptionals.B.of(4))
        .aFrom(ImmutableOptionals.D.of(false))
        .build();
  }

  protected Optionals.Host roundtrip(Optionals.Host obj) {
    String json = gson.toJson(obj);
    return gson.fromJson(json, Optionals.Host.class);
  }

  @Test
  public void emptyHost() {
    Optionals.Host h1 = createEmptyHost();
    Optionals.Host h2 = roundtrip(h1);
    check(h2).is(h1);
  }

  @Test
  public void populatedHost() {
    Optionals.Host h1 = createHost();
    Optionals.Host h2 = roundtrip(h1);
    check(h2).is(h1);
  }

  @Test
  public void fromJsonHost() {
    String json = "{"
        + "\"optInt\":3,"
        + "\"optDouble\":3.14,"
        + "\"optWrappedFloat\":3.14,"
        + "\"optWrappedInteger\":3,"
        + "\"optWrappedLong\":3,"
        + "\"optWrappedDouble\":3.14"
        + "}";
    Optionals.Host h1 = gson.fromJson(json, Optionals.Host.class);
    Optionals.Host h2 = roundtrip(h1);
    String finalJson = gson.toJson(h2);
    check(h2).is(h1);
    check(json).is(finalJson);
  }

  @Test
  public void serializedOptional() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapterFactory(new GsonAdaptersSerOpt())
        .registerTypeAdapter(Instant.class, new InstantSerializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .create();

    ImmutableSerOpt object = ImmutableSerOpt.builder()
        .instant(Instant.now())
        .build();

    String json = gson.toJson(object);

    check(json).is("{\"instant\":\"hello\"}");

    SerOpt opt = gson.fromJson("{\"instant\":\"whatever\"}", SerOpt.class);

    check(opt.instant().isPresent());
    check(opt.instant().get()).is(Instant.EPOCH);
  }

  // these serializer/deserializer are just to make sure they are called

  static public class InstantSerializer implements JsonSerializer<Instant> {
    @Override public JsonElement serialize(Instant instant, Type type, JsonSerializationContext context) {
      return new JsonPrimitive("hello");
    }
  }

  static public class InstantDeserializer implements JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
      return Instant.EPOCH;
    }
  }
}
