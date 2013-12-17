/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.common.marshal.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.common.marshal.Marshaler;

/**
 * The marshaling support.
 */
public final class MarshalingSupport {
  private MarshalingSupport() {}

  public static void ensureToken(JsonToken expected, JsonToken actual, Class<?> marshaledType) {
    if (expected != actual) {
      throw new UnmarshalMismatchException(marshaledType.getName(), "~", "", actual);
    }
  }

  /**
   * Ensure marshal condition.
   * @param condition the condition
   * @param hostType the host type
   * @param attributeName the attribute name
   * @param attributeType the attribute type
   * @param message the message
   */
  public static void ensureCondition(
      boolean condition,
      String hostType,
      String attributeName,
      String attributeType,
      Object message) {
    if (!condition) {
      throw new UnmarshalMismatchException(hostType, attributeName, attributeType, message);
    }
  }

  private static class UnmarshalMismatchException extends RuntimeException {
    private final String hostType;
    private final String attributeName;
    private final String attributeType;

    UnmarshalMismatchException(String hostType, String attributeName, String attributeType, Object message) {
      super(String.valueOf(message));
      this.hostType = hostType;
      this.attributeName = attributeName;
      this.attributeType = attributeType;
    }

    @Override
    public String getMessage() {
      return String.format("[%s.%s : %s] %s", hostType, attributeName, attributeType, super.getMessage());
    }
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <T> void marshalWithOneOfMarshalers(
      JsonGenerator generator,
      T instance,
      Marshaler<? extends T>... marshalers) throws IOException {
    for (Marshaler<?> marshaler : marshalers) {
      if (marshaler.getExpectedType().isInstance(instance)) {
        ((Marshaler<Object>) marshaler).marshalInstance(generator, instance);
      }
    }
  }

  /**
   * Support method that is used for parsing polymorphic/variant types.
   * @param parser the parser pointing to object start
   * @param hostType the host type
   * @param attributeName the attribute name
   * @param attributeType the attribute type
   * @param marshalers the marshalers to try
   * @return the object
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Object unmarshalWithOneOfMarshalers(
      JsonParser parser,
      String hostType,
      String attributeName,
      String attributeType,
      Marshaler<?>... marshalers) throws IOException {

    try (TokenBuffer buffer = new TokenBuffer(null)) { // intentional null
      buffer.copyCurrentStructure(parser);

      @Nullable
      List<RuntimeException> exceptions = Lists.newArrayListWithCapacity(marshalers.length);

      for (Marshaler<?> marshaler : marshalers) {
        try (JsonParser bufferParser = buffer.asParser()) {
          bufferParser.nextToken();
          return marshaler.unmarshalInstance(bufferParser);
        } catch (RuntimeException ex) {
          exceptions.add(ex);
        }
      }

      UnmarshalMismatchException exception =
          new UnmarshalMismatchException(hostType, attributeName, attributeType, "Cannot unambigously parse");

      for (RuntimeException ex : exceptions) {
        exception.addSuppressed(ex);
      }

      throw exception;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Marshaler<T> loadMarshalerFor(Class<? extends T> type) {
    @Nullable
    Class<?> marshaledType = extractBaseMarshaledType(type);
    Preconditions.checkArgument(marshaledType != null,
        "Type %s must have base type with @GenerateMarshaler annotation", type);

    try {
      ClassLoader classLoader = marshaledType.getClassLoader();
      Class<?> companionClass = classLoader.loadClass(marshaledType.getName() + "Marshaler");
      Method unmarshalMethod = companionClass.getMethod("instance");
      return (Marshaler<T>) unmarshalMethod.invoke(null);
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static boolean hasAssociatedMarshaler(Class<?> type) {
    return extractBaseMarshaledType(type) != null;
  }

  @Nullable
  private static Class<?> extractBaseMarshaledType(Class<?> type) {
    if (type.isAnnotationPresent(GenerateMarshaler.class)) {
      return type;
    }
    type = type.getSuperclass();
    if (type != null && type.isAnnotationPresent(GenerateMarshaler.class)) {
      return type;
    }
    return null;
  }
}
