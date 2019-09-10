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

package org.immutables.criteria;


import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TimeZone;

/**
 * Defines various types (long/int/boolean) with containers (like {@code int[]} or {@code List<Boolean>})
 */
@Value.Immutable
@Criteria
interface TypeHolder {

  @Value.Immutable
  @Criteria
  interface BooleanHolder {
    boolean value();
    Boolean boxed();
    @Nullable Boolean nullable();
    boolean[] array();
    List<Boolean> list();
    Optional<Boolean> optional();
  }

  @Value.Immutable
  @Criteria
  interface ByteHolder {
    byte value();
    Byte boxed();
    @Nullable Byte nullable();
    byte[] array();
    List<Byte> list();
    Optional<Byte> optional();
  }

  @Value.Immutable
  @Criteria
  interface ShortHolder {
    short value();
    Short boxed();
    @Nullable Short nullable();
    short[] array();
    List<Short> list();
    Optional<Short> optional();
  }

  @Value.Immutable
  @Criteria
  interface FloatHolder {
    float value();
    Float boxed();
    @Nullable Float nullable();
    float[] array();
    List<Float> list();
    Optional<Float> optional();
  }

  @Value.Immutable
  @Criteria
  interface DoubleHolder {
    double value();
    Double boxed();
    @Nullable Double nullable();
    double[] array();
    List<Double> list();
    OptionalDouble optional();
    Optional<Double> optional2();
  }

  @Value.Immutable
  @Criteria
  interface IntegerHolder {
    int value();
    Integer boxed();
    @Nullable Integer nullable();
    int[] array();
    List<Integer> list();
    OptionalInt optional();
    Optional<Integer> optional2();
  }

  @Value.Immutable
  @Criteria
  interface LongHolder {
    long value();
    Long boxed();
    @Nullable Long nullable();
    long[] array();
    List<Long> list();
    OptionalLong optional();
    Optional<Long> optional2();
  }

  @Value.Immutable
  @Criteria
  interface CharacterHolder {
    char value();
    Character boxed();
    @Nullable Character nullable();
    char[] array();
    List<Character> list();
    Optional<Character> optional();
  }

  @Value.Immutable
  @Criteria
  interface BigIntegerHolder {
    BigInteger value();
    @Nullable BigInteger nullable();
    BigInteger[] array();
    List<BigInteger> list();
    Optional<BigInteger> optional();
  }

  @Value.Immutable
  @Criteria
  interface BigDecimalHolder {
    BigDecimal value();
    @Nullable BigDecimal nullable();
    BigDecimal[] array();
    List<BigDecimal> list();
    Optional<BigDecimal> optional();
  }

  @Value.Immutable
  @Criteria
  interface StringHolder {
    String value();
    @Nullable String nullable();
    String[] array();
    List<String> list();
    Optional<String> optional();
  }

  enum Foo {
    ONE, TWO
  }

  @Value.Immutable
  @Criteria
  interface EnumHolder {
    Foo value();
    @Nullable Foo nullable();
    List<Foo> list();
    Foo[] array();
    Optional<Foo> optional();
  }

  @Value.Immutable
  @Criteria
  interface DateHolder {
    Date value();
    @Nullable Date nullable();
    List<Date> list();
    Date[] array();
    Optional<Date> optional();
  }

  @Value.Immutable
  @Criteria
  interface LocalDateHolder {
    LocalDate value();
    @Nullable LocalDate nullable();
    List<LocalDate> list();
    LocalDate[] array();
    Optional<LocalDate> optional();
  }

  @Value.Immutable
  @Criteria
  interface TimeZoneHolder {
    TimeZone value();
    @Nullable TimeZone nullable();
    List<TimeZone> list();
    TimeZone[] array();
    Optional<TimeZone> optional();
  }

  /**
   * Definitions which usually don't make sense
   */
  @Value.Immutable
  @Criteria
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
  interface MapHolder {
    Map<String, String> map();
    Optional<Map<String, String>> optionalMap();
    List<Map<String, String>> maps();

  }
}
