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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TimeZone;

@Value.Immutable
@Criteria
interface TypeHolder {

  // == primitives
  boolean booleanPrimitive();
  double doublePrimitive();
  float floatPrimitive();
  int intPrimitive();
  long longPrimitive();
  short shortPrimitive();
  byte bytePrimitive();
  char charPrimitive();

  // == boxed version of primitives
  Boolean booleanValue();
  Double doubleValue();
  Float floatValue();
  Integer intValue();
  Long longValue();
  Short shortValue();
  Byte byteValue();
  Character charValue();

  // == nullable version of boxed primitives
  @Nullable Boolean nullableBoolean();
  @Nullable Double nullableDouble();
  @Nullable Float nullableFloat();
  @Nullable Integer nullableInteger();
  @Nullable Long nullableLong();
  @Nullable Short nullableShort();
  @Nullable Byte nullableByte();
  @Nullable Character nullableCharacter();

  // == scalar optionals
  Optional<Boolean> optBoolean();
  OptionalDouble optDouble();
  Optional<Double> optDouble2();
  Optional<Float> optFloat();
  OptionalInt optInt();
  Optional<Integer> optInt2();
  OptionalLong optLong();
  Optional<Long> optLong2();
  Optional<Short> optShort();
  Optional<Byte> optByte();
  Optional<Character> optChar();

  // == lists
  List<Boolean> booleans();
  List<Double> doubles();
  List<Float> floats();
  List<Integer> integers();
  List<Long> longs();
  List<Short> shorts();
  List<Byte> bytes();
  List<Character> chars();

  // == arrays
  boolean[] booleanArray();
  double[] doubleArray();
  float[] floatArray();
  int[] integerArray();
  long[] longArray();
  short[] shortArray();
  byte[] byteArray();
  char[] charArray();

  enum Foo {
    ONE, TWO
  }

  Foo foo();
  Optional<Foo> optFoo();
  @Nullable Foo nullableFoo();
  List<Foo> foos();
  Foo[] fooArray();

  // java.time.*
  LocalDate localDate();
  Optional<LocalDate> optLocalDate();
  List<LocalDate> localDates();
  LocalDate[] localDateArray();

  // java.util.Date
  java.util.Date utilDate();
  Optional<java.util.Date> optUtilDate();
  List<java.util.Date> utilDates();
  java.util.Date[] utilDateArray();

  // BigInteger and BigDecimal
  BigInteger bigInteger();
  Optional<BigInteger> optBigInteger();
  @Nullable BigInteger nullableBigInteger();
  List<BigInteger> bigIntegers();
  BigInteger[] bigIntegerArray();

  BigDecimal bigDecimal();
  Optional<BigDecimal> optBigDecimal();
  @Nullable BigDecimal nullableBigDecimal();
  List<BigDecimal> bigDecimals();
  BigDecimal[] bigDecimalArray();

  // TimeZone
  TimeZone timeZone();
  Optional<TimeZone> optTimeZone();
  List<TimeZone> timeZones();
  TimeZone[] timeZoneArray();

  // weird stuff
  Optional<Optional<String>> weird1();
  Optional<List<String>> weird2();
  List<Optional<String>> weird3();
  Optional<OptionalInt> weird4();
  Optional<Map<String, String>> weird5();

  Map<String, String> map();
  Optional<Map<String, String>> optionalMap();
  List<Map<String, String>> maps();

}
