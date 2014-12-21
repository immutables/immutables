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
package org.immutables.common.marshal.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import java.io.IOException;
import java.math.BigDecimal;
import javax.annotation.Nullable;
import org.immutables.common.marshal.Marshaling;

/** Marshaling for built-in types and alike. */
public final class BuiltinMarshalingRoutines {
  private BuiltinMarshalingRoutines() {}

  /**
   * Fallback overload method that throws exception.
   * @param <T> the generic type
   * @param parser the parser
   * @param objectNull the null refence
   * @param expectedClass the expected class
   * @return the T instance
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static <T> T unmarshal(
      JsonParser parser,
      @Nullable Object objectNull,
      Class<?> expectedClass) throws IOException {
    @Nullable ObjectCodec codec = parser.getCodec();
    if (codec == null) {
      codec = Marshaling.getFallbackCodec();
    }
    if (codec != null) {
      @SuppressWarnings("unchecked") T value = (T) codec.readValue(parser, expectedClass);
      return value;
    }
    throw new IOException("No marshaler can handle " + expectedClass
        + ". Add appropriate mapping or you can set fallback codec:"
        + " Marshaling.Marshaling.setFallbackCodec()");
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param intNull the int null
   * @param expectedClass the expected class
   * @return the integer
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Integer unmarshal(
      JsonParser parser,
      @Nullable Integer intNull,
      Class<Integer> expectedClass) throws IOException {
    return parser.getIntValue();
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param longNull the long null
   * @param expectedClass the expected class
   * @return the long
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Long unmarshal(
      JsonParser parser,
      @Nullable Long longNull,
      Class<Long> expectedClass) throws IOException {
    return parser.getLongValue();
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param floatNull the float null
   * @param expectedClass the expected class
   * @return the float
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Float unmarshal(
      JsonParser parser,
      @Nullable Float floatNull,
      Class<Float> expectedClass) throws IOException {
    return parser.getFloatValue();
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param doubleNull the double null
   * @param expectedClass the expected class
   * @return the double
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Double unmarshal(
      JsonParser parser,
      @Nullable Double doubleNull,
      Class<Double> expectedClass) throws IOException {
    return parser.getDoubleValue();
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param charactedNull the characted null
   * @param expectedClass the expected class
   * @return the character
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Character unmarshal(
      JsonParser parser,
      @Nullable Character charactedNull,
      Class<Character> expectedClass) throws IOException {
    if (parser.getTextLength() != 1) {
      throw new IOException("Wrong Character value: " + parser.getText());
    }
    return parser.getTextCharacters()[parser.getTextOffset()];
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param booleanNull the boolean null
   * @param expectedClass the expected class
   * @return the boolean
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Boolean unmarshal(
      JsonParser parser,
      @Nullable Boolean booleanNull,
      Class<Boolean> expectedClass) throws IOException {
    return parser.getBooleanValue();
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param booleanNull the boolean null
   * @param expectedClass the expected class
   * @return the byte
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Byte unmarshal(
      JsonParser parser,
      @Nullable Byte booleanNull,
      Class<Byte> expectedClass) throws IOException {
    return parser.getByteValue();
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param shortNull the short null
   * @param expectedClass the expected class
   * @return the short
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Short unmarshal(
      JsonParser parser,
      @Nullable Short shortNull,
      Class<Byte> expectedClass) throws IOException {
    return parser.getShortValue();
  }

  /**
   * Default unmarshal for enum object.
   * <p>
   * Used in generated code via static imports method overload resolution by compiler.
   * @param <T> expected enum type
   * @param parser the parser
   * @param enumNull the enum null, always {@code null}
   * @param expectedClass the expected class
   * @return the T instance
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static <T extends Enum<T>> T unmarshal(
      JsonParser parser,
      @Nullable Enum<T> enumNull,
      Class<T> expectedClass) throws IOException {
    JsonToken t = parser.getCurrentToken();
    if (t != JsonToken.VALUE_STRING && t != JsonToken.FIELD_NAME) {
      MarshalingSupport.ensureToken(JsonToken.VALUE_STRING, t, expectedClass);
    }
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
   * Default unmarshal for BigDecimal.
   * <p>
   * Used in generated code via static imports method overload resolution by compiler.
   * @param parser the parser
   * @param numberNull the BigDecimal null, always {@code null}
   * @param expectedClass the expected class
   * @return the BigDecimal
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static BigDecimal unmarshal(
      JsonParser parser,
      @Nullable BigDecimal numberNull,
      Class<?> expectedClass) throws IOException {
    return parser.getDecimalValue();
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
      @Nullable ObjectCodec codec = generator.getCodec();
      if (codec == null) {
        codec = Marshaling.getFallbackCodec();
      }
      if (codec != null) {
        codec.writeValue(generator, instance);
      } else {
        generator.writeString(instance.toString());
      }
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
}
