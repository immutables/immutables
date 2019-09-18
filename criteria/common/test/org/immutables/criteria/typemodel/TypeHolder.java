/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.typemodel;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Defines various types (long/int/boolean) with containers (like {@code int[]} or {@code List<Boolean>})
 */
public interface TypeHolder {

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableBooleanHolder.class)
  @JsonSerialize(as = ImmutableBooleanHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface BooleanHolder {
    @Criteria.Id
    String id();
    boolean value();
    Boolean boxed();
    @Nullable Boolean nullable();
    Optional<Boolean> optional();
    boolean[] array();
    List<Boolean> list();

    static Supplier<ImmutableBooleanHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableBooleanHolder.builder().id("id" + counter.incrementAndGet()).value(counter.get() % 2 == 0)
              .nullable(counter.getAndIncrement() % 3 == 0 ? null : (counter.getAndIncrement() % 3 == 1))
              .boxed(counter.getAndIncrement() % 2 == 0 ? Boolean.TRUE : Boolean.FALSE)
              .array(true, false)
              .build();
    }
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  interface ByteHolder {
    byte value();
    Byte boxed();
    @Nullable Byte nullable();
    Optional<Byte> optional();
    byte[] array();
    List<Byte> list();
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  interface ShortHolder {
    short value();
    Short boxed();
    @Nullable Short nullable();
    Optional<Short> optional();
    short[] array();
    List<Short> list();
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  interface FloatHolder {
    float value();
    Float boxed();
    @Nullable Float nullable();
    Optional<Float> optional();
    float[] array();
    List<Float> list();
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableDoubleHolder.class)
  @JsonSerialize(as = ImmutableDoubleHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface DoubleHolder {
    @Criteria.Id
    String id();
    double value();
    Double boxed();
    @Nullable Double nullable();
    OptionalDouble optional();
    Optional<Double> optional2();
    double[] array();
    List<Double> list();

    static Supplier<ImmutableDoubleHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableDoubleHolder.builder().id("id" + counter.incrementAndGet()).value(counter.get()).boxed((double) counter.incrementAndGet()).array(1D, 2D).build();
    }

  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableIntegerHolder.class)
  @JsonSerialize(as = ImmutableIntegerHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface IntegerHolder {
    @Criteria.Id
    String id();
    int value();
    Integer boxed();
    @Nullable Integer nullable();
    Optional<Integer> optional2();
    int[] array();
    List<Integer> list();
    OptionalInt optional();

    static Supplier<ImmutableIntegerHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableIntegerHolder.builder().id("id" + counter.incrementAndGet()).value(counter.intValue()).nullable(counter.intValue()).boxed(counter.intValue()).array(1, 2).build();
    }

  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableLongHolder.class)
  @JsonSerialize(as = ImmutableLongHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface LongHolder {
    @Criteria.Id
    String id();
    long value();
    Long boxed();
    @Nullable Long nullable();
    OptionalLong optional();
    Optional<Long> optional2();
    long[] array();
    List<Long> list();

    static Supplier<ImmutableLongHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableLongHolder.builder().id("id" + counter.incrementAndGet()).value(counter.get()).boxed(counter.incrementAndGet()).array(1L, 2L).build();
    }
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableCharacterHolder.class)
  @JsonSerialize(as = ImmutableCharacterHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface CharacterHolder {
    @Criteria.Id
    String id();
    char value();
    Character boxed();
    @Nullable Character nullable();
    Optional<Character> optional();
    char[] array();
    List<Character> list();
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  interface BigIntegerHolder {
    BigInteger value();
    @Nullable BigInteger nullable();
    Optional<BigInteger> optional();
    BigInteger[] array();
    List<BigInteger> list();
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableBigDecimalHolder.class)
  @JsonSerialize(as = ImmutableBigDecimalHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface BigDecimalHolder {
    @Criteria.Id
    String id();
    BigDecimal value();
    @Nullable BigDecimal nullable();
    Optional<BigDecimal> optional();
    BigDecimal[] array();
    List<BigDecimal> list();
    static Supplier<ImmutableBigDecimalHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableBigDecimalHolder.builder().id("id" + counter.incrementAndGet()).value(BigDecimal.valueOf(counter.longValue())).nullable(null).array(BigDecimal.ONE).addList(BigDecimal.ZERO).build();
    }

  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableStringHolder.class)
  @JsonSerialize(as = ImmutableStringHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface StringHolder {
    @Criteria.Id
    String id();
    String value();
    @Nullable String nullable();
    Optional<String> optional();
    String[] array();
    List<String> list();

    static Supplier<ImmutableStringHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableStringHolder.builder().id("id" + counter.incrementAndGet()).value("foo").nullable(null).array("a1", "a2").addList("l1", "l2").build();
    }
  }

  enum Foo {
    ONE, TWO, THREE
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableEnumHolder.class)
  @JsonSerialize(as = ImmutableEnumHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface EnumHolder {
    @Criteria.Id
    String id();
    Foo value();
    @Nullable Foo nullable();
    Optional<Foo> optional();
    List<Foo> list();
    Foo[] array();
    static Supplier<ImmutableEnumHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableEnumHolder.builder().id("id" + counter.incrementAndGet()).value(Foo.values()[(int) (counter.get() % Foo.values().length)]).nullable(null).array(Foo.ONE).addList(Foo.TWO).build();
    }

  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableDateHolder.class)
  @JsonSerialize(as = ImmutableDateHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface DateHolder {
    @Criteria.Id
    String id();
    Date value();
    @Nullable Date nullable();
    Optional<Date> optional();
    List<Date> list();
    Date[] array();
    static Supplier<ImmutableDateHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableDateHolder.builder().id("id" + counter.incrementAndGet()).value(new Date()).nullable(null).array(new Date()).addList(new Date()).build();
    }
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableInstantHolder.class)
  @JsonSerialize(as = ImmutableInstantHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface InstantHolder {
    @Criteria.Id
    String id();
    Instant value();
    @Nullable Instant nullable();
    Optional<Instant> optional();
    List<Instant> list();
    Instant[] array();

    static Supplier<ImmutableInstantHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableInstantHolder.builder().id("id" + counter.incrementAndGet())
              .value(Instant.now())
              .nullable(null).array(Instant.now())
              .build();
    }
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableLocalDateTimeHolder.class)
  @JsonSerialize(as = ImmutableLocalDateTimeHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface LocalDateTimeHolder {
    @Criteria.Id
    String id();
    LocalDateTime value();
    @Nullable LocalDateTime nullable();
    Optional<LocalDateTime> optional();
    List<LocalDateTime> list();
    LocalDateTime[] array();

    static Supplier<ImmutableLocalDateTimeHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableLocalDateTimeHolder.builder().id("id" + counter.incrementAndGet())
              .value(LocalDateTime.now())
              .nullable(null).array(LocalDateTime.now())
              .build();
    }
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableLocalDateHolder.class)
  @JsonSerialize(as = ImmutableLocalDateHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface LocalDateHolder {
    @Criteria.Id
    String id();
    LocalDate value();
    @Nullable LocalDate nullable();
    Optional<LocalDate> optional();
    List<LocalDate> list();
    LocalDate[] array();

    static Supplier<ImmutableLocalDateHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableLocalDateHolder.builder().id("id" + counter.incrementAndGet())
              .value(LocalDate.now())
              .nullable(null).array(LocalDate.now()).build();
    }
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  @JsonDeserialize(as = ImmutableLocalTimeHolder.class)
  @JsonSerialize(as = ImmutableLocalTimeHolder.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface LocalTimeHolder {
    @Criteria.Id
    String id();
    LocalTime value();
    @Nullable LocalTime nullable();
    Optional<LocalTime> optional();
    List<LocalTime> list();
    LocalTime[] array();

    static Supplier<ImmutableLocalTimeHolder> generator() {
      AtomicLong counter = new AtomicLong();
      return () -> ImmutableLocalTimeHolder.builder().id("id" + counter.incrementAndGet())
              .value(LocalTime.now()).nullable(null).array(LocalTime.now()).build();
    }
  }


  @Value.Immutable
  @Criteria
  @Criteria.Repository
  interface TimeZoneHolder {
    TimeZone value();
    @Nullable TimeZone nullable();
    Optional<TimeZone> optional();
    List<TimeZone> list();
    TimeZone[] array();
  }

  /**
   * Definitions which usually don't make sense
   */
  @Value.Immutable
  @Criteria
  @Criteria.Repository
  interface WeirdHolder {
    // weird stuff
    Optional<Optional<String>> weird1();
    Optional<List<String>> weird2();
    List<Optional<String>> weird3();
    Optional<OptionalInt> weird4();
    Optional<Map<String, String>> weird5();
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository
  interface MapHolder {
    Map<String, String> map();
    Optional<Map<String, String>> optionalMap();
    List<Map<String, String>> maps();
  }
}
