/*
   Copyright 2015-2021 Immutables Authors and Contributors

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

import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.immutables.gson.stream.JsonParserReader;

/**
 * Composite type adapter used for polymorphic serialization.
 * It just buffers input and try each of delegating type adapters until succeeded. While there may
 * be legit concern about performance, it is most flexible way to handle subtypes by structure and
 * not by discriminator fields of some sort.
 * If failed with all type adapters exception will be thrown, attaching suppressed exceptions for
 * individual delegate adapters.
 * @param <T> supertype to adapt
 */
public final class ExpectedSubtypesAdapter<T> extends TypeAdapter<T> {
  private final TypeToken<T> type;
  private final Gson gson;
  private final List<TypeAdapter<? extends T>> adapters;
  private final TypeToken<? extends T>[] subtypes;

  /**
   * Creates adapter from {@link Gson} and type tokens.
   * @param <T> the generic type
   * @param gson Gson instance
   * @param type supertype token
   * @param subtypes subtype tokens
   * @return subtype adapter
   */
  @SafeVarargs
  public static <T> ExpectedSubtypesAdapter<T> create(
      Gson gson,
      TypeToken<T> type,
      TypeToken<? extends T>... subtypes) {
    return new ExpectedSubtypesAdapter<>(gson, type, subtypes);
  }

  @SafeVarargs
  public static <T> ExpectedSubtypesAdapter<T> create(
      Gson gson,
      Class<T> type,
      TypeToken<? extends T>... subtypes) {
    return create(gson, TypeToken.get(type), subtypes);
  }

  private ExpectedSubtypesAdapter(Gson gson, TypeToken<T> type, TypeToken<? extends T>[] subtypes) {
    if (subtypes.length < 1) {
      throw new IllegalArgumentException("At least one subtype should be specified");
    }
    if (gson == null) {
      throw new NullPointerException("supplied Gson is null");
    }
    if (type == null) {
      throw new NullPointerException("supplied type Gson is null");
    }
    this.gson = gson;
    this.type = type;
    this.subtypes = subtypes.clone();
    this.adapters = lookupAdapters();
  }

  private List<TypeAdapter<? extends T>> lookupAdapters() {
    List<TypeAdapter<? extends T>> adapters = new ArrayList<>(subtypes.length);
    for (TypeToken<? extends T> subtype : subtypes) {
      adapters.add(gson.getAdapter(subtype));
    }
    return adapters;
  }

  public TypeToken<T> getType() {
    return type;
  }

  @Override
  public void write(JsonWriter out, T value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    for (int i = 0; i < subtypes.length; i++) {
      TypeToken<? extends T> subtype = subtypes[i];
      if (subtype.getRawType().isInstance(value)) {
        // safe unchecked, type is checked at runtime
        @SuppressWarnings("unchecked") TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) adapters.get(i);
        typeAdapter.write(out, value);
        return;
      }
    }
    gson.toJson(value, value.getClass(), out);
  }

  private interface ReadAttempt {
    Object read(TypeAdapter<?> typeAdapter) throws Exception;
  }

  // safe unchecked. expected that supplied adapters are producing correct types
  // enforced by constructor parameters.
  @SuppressWarnings("unchecked")
  @Override
  public T read(JsonReader in) throws IOException {
    List<Exception> exceptions = new ArrayList<>(subtypes.length);
    ReadAttempt attempt = readInAttempt(in);
    for (TypeAdapter<?> typeAdapter : adapters) {
      try {
        return (T) attempt.read(typeAdapter);
      } catch (Exception ex) {
        exceptions.add(ex);
      }
    }
    JsonParseException failure = new JsonParseException(
        String.format(
            "Cannot parse %s with following subtypes: %s",
            type,
            Arrays.toString(subtypes)));
    for (Exception exception : exceptions) {
      failure.addSuppressed(exception);
    }
    throw failure;
  }

  private ReadAttempt readInAttempt(JsonReader in) throws IOException {
    // check Callable marker for Jackson implementation
    if (in instanceof Callable<?>) {
      // if done inside 'if' we would not leak runtime dependency on Jackson
      if (in instanceof JsonParserReader) {
        return newJacksonAttempt((JsonParserReader) in);
      }
    }
    return newGsonAttempt(in);
  }

  /**
   * Default Jackson implementation
   */
  private ReadAttempt newGsonAttempt(JsonReader in) throws IOException {
    JsonElement element = gson.getAdapter(JsonElement.class).read(in);
    return new ReadAttempt() {
      @Override public Object read(TypeAdapter<?> typeAdapter) throws Exception {
        return typeAdapter.fromJsonTree(element);
      }
    };
  }

  /**
   * Jackson buffer copy attempt. Use of Jackson's own mechanisms is important to preserve custom elements
   * such as special embedded objects in BSON or other data formats. Jackson classes should not leak outside
   * of this' class methods. So when there's no Jackson available in classpath, it will still work
   * with Gson based implementation.
   */
  private ReadAttempt newJacksonAttempt(JsonParserReader in) throws IOException {
    TokenBuffer buffer = ((JsonParserReader) in).nextTokenBuffer();
    return new ReadAttempt() {
      @Override public Object read(TypeAdapter<?> typeAdapter) throws Exception {
        try(JsonParserReader reader = new JsonParserReader(buffer.asParser())) {
          return typeAdapter.read(reader);
        }
      }
    };
  }
}
