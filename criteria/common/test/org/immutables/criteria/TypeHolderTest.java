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
            .intPrimitive.isEqualTo(0)
            .intPrimitive.isGreaterThan(22)
            .longPrimitive.isLessThan(22L)
            .longPrimitive.isIn(1L, 2L, 3L)
            .charPrimitive.isEqualTo('A')
            .doublePrimitive.isGreaterThan(1.1)
            .doublePrimitive.isIn(1D, 2D, 3D)
            .floatPrimitive.isEqualTo(33F)
            .floatPrimitive.isGreaterThan(12F)
            .shortPrimitive.isGreaterThan((short) 2)
            .bytePrimitive.isNotEqualTo((byte) 0);

    // == Optionals
    TypeHolderCriteria.typeHolder
            .optBoolean.value().isFalse()
            .optBoolean.value().isTrue()
            .optBoolean.isAbsent()
            .optInt.isAbsent()
            .optLong.isAbsent()
            .optLong.value().isLessThan(11L)
            .optLong2.isAbsent()
            .optLong2.value().isLessThan(22L)
            .optShort.isPresent()
            .optDouble.isPresent()
            .optDouble.value().isGreaterThan(22D)
            .optDouble2.value().isLessThan(11D)
            .optFloat.isAbsent()
            .optShort.value().isLessThan((short) 22)
            .optShort.isAbsent();

    // == Boxed
    TypeHolderCriteria.typeHolder
            .doubleValue.isLessThan(22D)
            .booleanValue.isTrue()
            .booleanValue.isFalse()
            .intValue.isLessThan(22)
            .doubleValue.isLessThan(1D)
            .shortValue.isLessThan((short) 11)
            .byteValue.isGreaterThan((byte) 2)
            .longValue.isLessThan(44L);

    // == lists
    TypeHolderCriteria.typeHolder
            .booleans.any().isTrue()
            .booleans.isNotEmpty()
            .booleans.hasSize(1)
            .bytes.none().isEqualTo((byte) 0)
            .bytes.hasSize(1)
            .shorts.any().isEqualTo((short) 22)
            .shorts.hasSize(1)
            .integers.any().isAtLeast(11)
            .integers.any(i -> i.isLessThan(22))
            .integers.isNotEmpty()
            .integers.hasSize(1)
            .longs.none().isGreaterThan(11L)
            .longs.none(l -> l.isGreaterThan(22L).isLessThan(23L))
            .longs.hasSize(2)
            .doubles.none().isLessThan(1D)
            .doubles.hasSize(2)
            .floats.all().isGreaterThan(22F)
            .chars.isEmpty()
            .chars.any().isGreaterThan('A')
            .chars.none(c -> c.isIn('a', 'b', 'c').isLessThan('t'));
  }

  @Test
  public void dates() {
    TypeHolderCriteria.typeHolder
            .localDate.isAtMost(LocalDate.MIN)
            .optLocalDate.value().isAtMost(LocalDate.MAX)
            .optLocalDate.value(d -> d.isAtMost(LocalDate.MAX))
            .localDates.contains(LocalDate.MAX)
            .localDates.all(d -> d.isLessThan(LocalDate.MIN));
  }

  @Test
  public void javaUtilDate() {

    final Date date = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10));

    TypeHolderCriteria.typeHolder
            .utilDate.isAtMost(date)
            .optUtilDate.value().isAtMost(date)
            .utilDates.all().isAtLeast(date);
  }

  /**
   * Test for BigInteger and BigDecimal
   */
  @Test
  public void bigIntegerAndDecimal() {
    TypeHolderCriteria.typeHolder
            .bigDecimal.isAtLeast(BigDecimal.ONE)
            .optBigDecimal.value().isAtLeast(BigDecimal.ONE)
            .optBigDecimal.value(b -> b.isGreaterThan(BigDecimal.TEN))
            .bigDecimals.contains(BigDecimal.TEN)
            .bigDecimals.isNotEmpty()
            .bigDecimals.any().isAtLeast(BigDecimal.ONE);

    TypeHolderCriteria.typeHolder
            .bigInteger.isAtLeast(BigInteger.ONE)
            .optBigInteger.value().isAtLeast(BigInteger.ONE)
            .optBigInteger.value(b -> b.isGreaterThan(BigInteger.TEN))
            .bigIntegers.contains(BigInteger.TEN)
            .bigIntegers.isNotEmpty()
            .bigIntegers.any().isAtLeast(BigInteger.ONE);
  }

  @Test
  public void enumCheck() {
      TypeHolderCriteria.typeHolder
              .foos.none().isEqualTo(TypeHolder.Foo.TWO)
              .foos.none(e -> e.isNotEqualTo(TypeHolder.Foo.ONE).isLessThan(TypeHolder.Foo.TWO))
              .foo.isEqualTo(TypeHolder.Foo.ONE)
              .optFoo.isPresent()
              .optFoo.value().isEqualTo(TypeHolder.Foo.ONE)
              .optFoo.value(e -> e.isIn(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO));
  }

  @Test
  public void timeZones() {
    TypeHolderCriteria.typeHolder
            .timeZone.isEqualTo(TimeZone.getDefault())
            .optTimeZone.value().isEqualTo(TimeZone.getDefault())
            .timeZones.contains(TimeZone.getDefault());
  }
}
