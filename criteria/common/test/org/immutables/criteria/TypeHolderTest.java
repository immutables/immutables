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

import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Used only for compilation tests. Not executed at runtime.
 */
@Ignore
public class TypeHolderTest {


  @Test
  public void name() {
    // primitives
    TypeHolderCriteria.typeHolder
            .booleanPrimitive.isTrue()
            .booleanPrimitive.isFalse()
            .intPrimitive.is(0)
            .intPrimitive.greaterThan(22)
            .longPrimitive.lessThan(22L)
            .longPrimitive.in(1L, 2L, 3L)
            .charPrimitive.is('A')
            .doublePrimitive.greaterThan(1.1)
            .doublePrimitive.in(1D, 2D, 3D)
            .floatPrimitive.is(33F)
            .floatPrimitive.greaterThan(12F)
            .shortPrimitive.greaterThan((short) 2)
            .bytePrimitive.isNot((byte) 0);

    // == Optionals
    TypeHolderCriteria.typeHolder
            .optBoolean.value().isFalse()
            .optBoolean.value().isTrue()
            .optBoolean.isAbsent()
            .optInt.isAbsent()
            .optLong.isAbsent()
            .optLong.value().lessThan(11L)
            .optLong2.isAbsent()
            .optLong2.value().lessThan(22L)
            .optShort.isPresent()
            .optDouble.isPresent()
            .optDouble.value().greaterThan(22D)
            .optDouble2.value().lessThan(11D)
            .optFloat.isAbsent()
            .optShort.value().lessThan((short) 22)
            .optShort.isAbsent();

    // == Boxed
    TypeHolderCriteria.typeHolder
            .doubleValue.lessThan(22D)
            .booleanValue.isTrue()
            .booleanValue.isFalse()
            .intValue.lessThan(22)
            .doubleValue.lessThan(1D)
            .shortValue.lessThan((short) 11)
            .byteValue.greaterThan((byte) 2)
            .longValue.lessThan(44L);

    // == lists
    TypeHolderCriteria.typeHolder
            .booleans.any().isTrue()
            .booleans.notEmpty()
            .booleans.hasSize(1)
            .bytes.none().is((byte) 0)
            .bytes.hasSize(1)
            .shorts.any().is((short) 22)
            .shorts.hasSize(1)
            .integers.any().atLeast(11)
            .integers.notEmpty()
            .integers.hasSize(1)
            .longs.none().greaterThan(11L)
            .longs.hasSize(2)
            .doubles.none().lessThan(1D)
            .doubles.hasSize(2)
            .floats.all().greaterThan(22F)
            .chars.isEmpty()
            .chars.any().greaterThan('A');
  }

  @Test
  public void arrays() {
    TypeHolderCriteria.typeHolder
            .booleanArray.isEmpty()
            .booleanArray.hasSize(11)
            .booleanArray.contains(true)
            .bigIntegerArray.contains(BigInteger.ONE)
            .bigDecimalArray.contains(BigDecimal.ONE)
            .fooArray.contains(TypeHolder.Foo.ONE)
            .timeZoneArray.contains(TimeZone.getDefault())
            .localDateArray.contains(LocalDate.MAX)
            .utilDateArray.contains(new java.util.Date());
  }

  @Test
  public void dates() {
    TypeHolderCriteria.typeHolder
            .localDate.atMost(LocalDate.MIN)
            .optLocalDate.value().atMost(LocalDate.MAX)
            .localDates.contains(LocalDate.MAX);
  }

  @Test
  public void javaUtilDate() {

    final Date date = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10));

    TypeHolderCriteria.typeHolder
            .utilDate.atMost(date)
            .optUtilDate.value().atMost(date)
            .utilDates.all().atLeast(date);
  }

  /**
   * Test for BigInteger and BigDecimal
   */
  @Test
  public void bigIntegerAndDecimal() {
    TypeHolderCriteria.typeHolder
            .bigDecimal.atLeast(BigDecimal.ONE)
            .optBigDecimal.value().atLeast(BigDecimal.ONE)
            .bigDecimals.contains(BigDecimal.TEN)
            .bigDecimals.notEmpty()
            .bigDecimals.any().atLeast(BigDecimal.ONE);

    TypeHolderCriteria.typeHolder
            .bigInteger.atLeast(BigInteger.ONE)
            .optBigInteger.value().atLeast(BigInteger.ONE)
            .bigIntegers.contains(BigInteger.TEN)
            .bigIntegers.notEmpty()
            .bigIntegers.any().atLeast(BigInteger.ONE);
  }

  @Test
  public void enumCheck() {
      TypeHolderCriteria.typeHolder
              .foos.none().is(TypeHolder.Foo.TWO)
              .foo.is(TypeHolder.Foo.ONE)
              .optFoo.isPresent()
              .optFoo.value().is(TypeHolder.Foo.ONE);
  }

  @Test
  public void timeZones() {
    TypeHolderCriteria.typeHolder
            .timeZone.is(TimeZone.getDefault())
            .optTimeZone.value().is(TimeZone.getDefault())
            .timeZones.contains(TimeZone.getDefault());
  }
}
