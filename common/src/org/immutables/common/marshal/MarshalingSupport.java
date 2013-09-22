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
package org.immutables.common.marshal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import javax.annotation.Nullable;

//TODO move to .internal
/**
 * The marshaling support.
 */
public final class MarshalingSupport {
  private MarshalingSupport() {
  }

  /**
   * Default unmarshal for enum object.
   * <p>
   * Used in generated code via static imports method overload resolution by compiler.
   * @param <T> expected enum type
   * @param parser the parser
   * @param enumNull the enum null, always {@code null}
   * @param expectedClass the expected class
   * @return the t
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static <T extends Enum<T>> T unmarshal(
      JsonParser parser,
      @Nullable Enum<T> enumNull,
      Class<T> expectedClass) throws IOException {
    return Enum.valueOf(expectedClass, parser.getText());
  }

  /**
   * Default unmarshal for String.
   * <p>
   * Used in generated code via static imports method overload resolution by compiler.
   * @param parser the parser
   * @param stringNull the string null, always {@code null}
   * @param expectedClass the expected class
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static String unmarshal(
      JsonParser parser,
      @Nullable String stringNull,
      Class<?> expectedClass) throws IOException {
    return parser.getText();
  }

  /**
   * Marshal key by default via {@link Object#toString()}.
   * @param object the object
   * @return the string
   */
  public static String marshalKey(Object object) {
    return object.toString();
  }

  /**
   * Default catch-all marshal for objects, does {@link Object#toString()} or writes null-literal if
   * object is {@code null}.
   * @param generator the generator
   * @param instance the instance
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      @Nullable Object instance) throws IOException {
    if (instance == null) {
      generator.writeNull();
    } else {
      generator.writeString(instance.toString());
    }
  }

  /**
   * Default marshal for {@link BigDecimal}, does {@link JsonGenerator#writeNumber(BigDecimal)}.
   * @param generator the generator
   * @param instance the instance
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      BigDecimal instance) throws IOException {
    generator.writeNumber(instance);
  }

  /**
   * Default marshal for any other {@link Number}, does {@link JsonGenerator#writeNumber(double)}
   * for {@link Number#doubleValue()}.
   * @param generator the generator
   * @param instance the instance
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      Number instance) throws IOException {
    generator.writeNumber(instance.doubleValue());
  }

  /**
   * Default marshal for {@link Enum}, does {@link JsonGenerator#writeString(String)} for.
   * @param generator the generator
   * @param instance the instance
   * @throws IOException Signals that an I/O exception has occurred. {@link Enum#name()}.
   */
  public static void marshal(
      JsonGenerator generator,
      Enum<?> instance) throws IOException {
    generator.writeString(instance.name());
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      byte value) throws IOException {
    generator.writeNumber(value);
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      char value) throws IOException {
    generator.writeString(String.valueOf(value));
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      short value) throws IOException {
    generator.writeNumber(value);
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      int value) throws IOException {
    generator.writeNumber(value);
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      long value) throws IOException {
    generator.writeNumber(value);
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      float value) throws IOException {
    generator.writeNumber(value);
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      double value) throws IOException {
    generator.writeNumber(value);
  }

  /**
   * Marshal.
   * @param generator the generator
   * @param value the value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void marshal(
      JsonGenerator generator,
      boolean value) throws IOException {
    generator.writeBoolean(value);
  }

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

    TokenBuffer buffer = new TokenBuffer(null); // intentional null
    buffer.copyCurrentStructure(parser);

    @Nullable
    List<RuntimeException> exceptions = Lists.newArrayListWithCapacity(marshalers.length);

    for (Marshaler<?> marshaler : marshalers) {
      try {
        JsonParser bufferParser = buffer.asParser();
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
