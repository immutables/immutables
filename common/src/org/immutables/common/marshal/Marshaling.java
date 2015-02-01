/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.common.marshal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import java.io.IOException;
import java.io.StringWriter;
import javax.annotation.Nullable;
import org.immutables.common.marshal.internal.MarshalingSupport;

/**
 * Contains convenient methods for marshaling and unmarshaling documents annotated with
 * {@code Json.Marshaled} to and from standard textual JSON.
 * <p>
 * You can avoid using this class in favor of using Marshalers directly. But It's not always
 * possible if dynamic lookup is needed.
 */
public final class Marshaling {
  private Marshaling() {}

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  @Nullable
  private static volatile ObjectCodec fallbackCodec;

  /**
   * Allows to inialize statically shared object codec, which will be used a last resort for
   * object marshaling. Prefer to use marshaling routines or mapped immutable objects.
   * <p>
   * 
   * <pre>
   * Marshaling.setObjectCodec(new ObjectMapper());
   * </pre>
   * @param fallbackCodec the new default codec
   */
  public static void setFallbackCodec(@Nullable ObjectCodec fallbackCodec) {
    Marshaling.fallbackCodec = fallbackCodec;
  }

  /**
   * Currently configured via {@link #setFallbackCodec(ObjectCodec)} fallback codec.
   * @return fallback codec or {@code null}
   */
  @Nullable
  public static ObjectCodec getFallbackCodec() {
    return fallbackCodec;
  }

  /**
   * Marshal object to JSON. Output JSON string is pretty-printed.
   * @param object the object
   * @return JSON string
   */
  // safe to not care of in-memory resource closing, leave everything to GC
  @SuppressWarnings("resource")
  public static String toJson(Object object) {
    Marshaler<Object> marshaler = marshalerFor(object.getClass());
    try {
      StringWriter writer = new StringWriter();
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      generator.useDefaultPrettyPrinter();
      marshaler.marshalInstance(generator, object);
      generator.close();
      return writer.toString();
    } catch (IOException ex) {
      throw toRuntimeException(ex);
    }
  }

  private static RuntimeException toRuntimeException(IOException ex) {
    RuntimeException problem = new RuntimeException(ex.getMessage());
    problem.setStackTrace(ex.getStackTrace());
    if (ex.getCause() != null) {
      problem.initCause(ex.getCause());
    }
    throw problem;
  }

  /**
   * Unmarshal object from JSON by specifying expected type.
   * @param <T> the exected type
   * @param json JSON string
   * @param expectedType the expected type class
   * @return the unmarshaled instance
   */
  // safe to not care of in-memory resource closing, which will be handled by GC
  @SuppressWarnings("resource")
  public static <T> T fromJson(String json, Class<? extends T> expectedType) {
    Marshaler<T> marshaler = marshalerFor(expectedType);
    try {
      JsonParser parser = JSON_FACTORY.createParser(json);
      return marshaler.unmarshalInstance(parser);
    } catch (IOException ex) {
      throw toRuntimeException(ex);
    }
  }

  /**
   * Loads and caches marshaler for the specified expected type.
   * Expected type should be either abstract value class annotated with
   * {@link org.immutables.value.Value.Immutable},
   * or actual generated immutable subclass.
   * @param <T> expected type
   * @param expectedType expected type to marshal
   * @return marshaler
   */
  public static <T> Marshaler<T> marshalerFor(Class<? extends T> expectedType) {
    return MarshalingSupport.getMarshalerFor(expectedType);
  }
}
