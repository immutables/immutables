package org.immutables.criteria;


import org.immutables.value.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

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

  enum Foo {
    ONE, TWO
  }

  Foo foo();
  Optional<Foo> optFoo();
  List<Foo> foos();

  // java.time.*
  LocalDate localDate();
  Optional<LocalDate> optLocalDate();
  List<LocalDate> localDates();

  // java.util.Date
  java.util.Date utilDate();
  Optional<java.util.Date> optUtilDate();
  List<java.util.Date> utilDates();


}
