package org.immutables.common.marshal.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.types.ObjectId;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.common.repository.Id;
import org.immutables.common.time.TimeInstant;
import org.immutables.common.time.TimeMeasure;

/** Marshaling for built-in types and alike. */
public final class BuiltinMarshalingRoutines {
  private BuiltinMarshalingRoutines() {}

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

  // Marshalers of some special types for repositories goes here
  // They are better factored out to separate set of routines (i.e. another class)

  // Id
  /**
   * Unmarshal.
   * @param parser the parser
   * @param idNull the id null
   * @param expectedClass the expected class
   * @return the id
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Id unmarshal(
      JsonParser parser,
      @Nullable Id idNull,
      Class<Id> expectedClass) throws IOException {
    JsonToken token = parser.getCurrentToken();
    if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
      ObjectId objectId = (ObjectId) parser.getEmbeddedObject();
      return Id.of(
          objectId.getTime(),
          objectId.getMachine(),
          objectId.getInc());
    }

    return Id.fromString(parser.getText());
  }

  public static void marshal(
      JsonGenerator generator,
      Id value) throws IOException {
    if (generator instanceof BsonGenerator) {
      ((BsonGenerator) generator).writeObjectId(new ObjectId(
          value.time(),
          value.machine(),
          value.inc()));
    } else {
      generator.writeString(value.toString());
    }
  }

  // TimeInstant
  /**
   * Unmarshal.
   * @param parser the parser
   * @param instantNull the instant null
   * @param expectedClass the expected class
   * @return the time instant
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static TimeInstant unmarshal(
      JsonParser parser,
      @Nullable TimeInstant instantNull,
      Class<TimeInstant> expectedClass) throws IOException {

    JsonToken token = parser.getCurrentToken();
    if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
      Date date = (Date) parser.getEmbeddedObject();
      return TimeInstant.of(date.getTime());
    }

    return TimeInstant.of(parser.getLongValue());
  }

  public static void marshal(
      JsonGenerator generator,
      TimeInstant value) throws IOException {

    if (generator instanceof BsonGenerator) {
      ((BsonGenerator) generator).writeDateTime(new Date(value.value()));
    } else {
      generator.writeNumber(value.value());
    }
  }

  // TimeMeasure
  /**
   * Unmarshal.
   * @param parser the parser
   * @param instantNull the instant null
   * @param expectedClass the expected class
   * @return the time measure
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static TimeMeasure unmarshal(
      JsonParser parser,
      @Nullable TimeMeasure instantNull,
      Class<TimeMeasure> expectedClass) throws IOException {
    return TimeMeasure.fromString(parser.getText());
  }

  public static void marshal(
      JsonGenerator generator,
      TimeMeasure value) throws IOException {
    generator.writeString(value.toString());
  }
}
