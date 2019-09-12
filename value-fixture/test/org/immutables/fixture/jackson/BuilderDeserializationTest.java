package org.immutables.fixture.jackson;

import static org.immutables.check.Checkers.check;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

public class BuilderDeserializationTest {
  @Value.Immutable
  abstract static class IntValueClass {

    @JsonValue
    abstract Integer getValue();

    @JsonCreator
    static IntValueClass of(final Integer value) {
      return ImmutableIntValueClass.builder().value(value).build();
    }
  }

  @Value.Immutable
  @JsonDeserialize(as = ImmutableWorkingType.class)
  static abstract class WorkingType {

    @JsonProperty("int_value_property")
    @JsonSerialize(converter = IntValueOverrideConverter.StringSerializer.class)
    @JsonDeserialize(converter = IntValueOverrideConverter.StringDeserializer.class)
    abstract IntValueClass getIntValue();
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableBrokenType.Builder.class)
  static abstract class BrokenType {

    @JsonProperty("int_value_property")
    @JsonSerialize(converter = IntValueOverrideConverter.StringSerializer.class)
    @JsonDeserialize(converter = IntValueOverrideConverter.StringDeserializer.class)
    abstract IntValueClass getIntValue();
  }

  static class IntValueOverrideConverter {
    static class StringSerializer extends StdConverter<IntValueClass, String> {
      @Override
      public String convert(final IntValueClass value) {
        return value.getValue().toString();
      }
    }

    static class StringDeserializer extends StdConverter<String, IntValueClass> {
      @Override
      public IntValueClass convert(final String value) {
        return IntValueClass.of(Integer.parseInt(value));
      }
    }
  }

  final ObjectMapper MAPPER = new ObjectMapper();
  final IntValueClass INT_VALUE = IntValueClass.of(1);

  @Test
  public void workingTest() throws Exception {
    WorkingType someType = ImmutableWorkingType.builder()
        .intValue(INT_VALUE)
        .build();

    String serialized = MAPPER.writeValueAsString(someType);
    check(serialized).is("{\"int_value_property\":\"1\"}");

    WorkingType someType2 = MAPPER.readValue(serialized, WorkingType.class);
    check(someType2).is(someType);
  }

  @Test
  public void brokenTest() throws Exception {
    BrokenType someType = ImmutableBrokenType.builder()
        .intValue(INT_VALUE)
        .build();

    String serialized = MAPPER.writeValueAsString(someType);
    check(serialized).is("{\"int_value_property\":\"1\"}");

    BrokenType someType2 = MAPPER.readValue(serialized, BrokenType.class);
    check(someType2).is(someType);
  }
}
