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

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Validating criteria for different types
 */
public class TypeHolderTest {

  @Test
  public void booleanValue() {
    BooleanHolderCriteria.booleanHolder
            .value.isTrue()
            .value.isFalse()
            .value.is(true)
            .value.is(false)
            .boxed.isTrue()
            .boxed.isFalse()
            .optional.is(true)
            .nullable.isAbsent()
            .nullable.isTrue()
            .nullable.is(true);
  }

  @Test
  public void string() {
    StringHolderCriteria.stringHolder
            .value.is("abc")
            .optional.is("foo")
            .optional.isAbsent()
            .optional.isPresent()
            .nullable.is("abc")
            .nullable.isPresent()
            .nullable.isAbsent()
            .list.contains("aaa")
            .array.contains("bbb");
  }

  @Test
  public void dates() {
    LocalDateHolderCriteria.localDateHolder
            .value.is(LocalDate.now())
            .value.atMost(LocalDate.MIN)
            .optional.atMost(LocalDate.MAX)
            .nullable.is(LocalDate.MAX)
            .list.contains(LocalDate.MAX)
            .not(d -> d.array.isEmpty());
  }

  @Test
  public void javaUtilDate() {
    final Date date = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10));
    DateHolderCriteria.dateHolder
            .value.is(date)
            .value.atMost(date)
            .value.atLeast(date)
            .array.contains(date)
            .list.contains(date)
            .optional.atMost(date)
            .optional.is(date)
            .optional.isAbsent()
            .optional.isAbsent()
            .nullable.isAbsent()
            .nullable.atLeast(date)
            .nullable.is(date)
            .nullable.atMost(date)
            .nullable.isPresent();
  }

  /**
   * Test for BigInteger and BigDecimal
   */
  @Test
  public void bigInteger() {
    BigIntegerHolderCriteria.bigIntegerHolder
            .value.atLeast(BigInteger.ONE)
            .value.is(BigInteger.ONE)
            .optional.is(BigInteger.ONE)
            .optional.atLeast(BigInteger.ONE)
            .nullable.is(BigInteger.ONE)
            .list.contains(BigInteger.ONE)
            .array.contains(BigInteger.ONE)
            .list.notEmpty()
            .array.isEmpty();
  }

  @Test
  public void bigDecimal() {
    BigDecimalHolderCriteria.bigDecimalHolder
            .value.is(BigDecimal.ONE)
            .value.atLeast(BigDecimal.ONE)
            .optional.atLeast(BigDecimal.ONE)
            .nullable.is(BigDecimal.ONE)
            .list.contains(BigDecimal.ONE)
            .array.contains(BigDecimal.ONE)
            .list.notEmpty()
            .array.isEmpty();
  }

  @Test
  public void enumCheck() {
    EnumHolderCriteria.enumHolder
              .list.hasSize(1)
              .array.hasSize(1)
              .value.is(TypeHolder.Foo.ONE)
              .value.isNot(TypeHolder.Foo.ONE)
              .nullable.isAbsent()
              .nullable.is(TypeHolder.Foo.ONE)
              .optional.isPresent()
              .optional.is(TypeHolder.Foo.ONE);
  }

  @Test
  public void timeZones() {
    TimeZoneHolderCriteria.timeZoneHolder
            .value.is(TimeZone.getDefault())
            .optional.value().is(TimeZone.getDefault())
            .nullable.value().is(TimeZone.getDefault())
            .array.contains(TimeZone.getDefault())
            .list.contains(TimeZone.getDefault());
  }
}
