package org.immutables.criteria;

import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Used only for compilation tests. Not executed at runtime.
 */
@Ignore
public class TypeHolderTest {


  @Test
  public void name() {
    // primitives
    TypeHolderCriteria.create()
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
    TypeHolderCriteria.create()
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
    TypeHolderCriteria.create()
            .doubleValue.isLessThan(22D)
            .booleanValue.isTrue()
            .booleanValue.isFalse()
            .intValue.isLessThan(22)
            .doubleValue.isLessThan(1D)
            .shortValue.isLessThan((short) 11)
            .byteValue.isGreaterThan((byte) 2)
            .longValue.isLessThan(44L);

    // == lists
    TypeHolderCriteria.create()
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
    TypeHolderCriteria.create()
            .localDate.isAtMost(LocalDate.MIN)
            .optLocalDate.value().isAtMost(LocalDate.MAX)
            .optLocalDate.value(d -> d.isAtMost(LocalDate.MAX))
            .localDates.contains(LocalDate.MAX)
            .localDates.all(d -> d.isLessThan(LocalDate.MIN));
  }

  @Test
  public void javaUtilDate() {

    final Date date = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10));

    TypeHolderCriteria.create()
            .utilDate.isAtMost(date)
            .optUtilDate.value().isAtMost(date)
            .utilDates.all().isAtLeast(date);
  }

  @Test
  public void enumCheck() {
      TypeHolderCriteria.create()
              .foos.none().isEqualTo(TypeHolder.Foo.TWO)
              .foos.none(e -> e.isNotEqualTo(TypeHolder.Foo.ONE).isLessThan(TypeHolder.Foo.TWO))
              .foo.isEqualTo(TypeHolder.Foo.ONE)
              .optFoo.isPresent()
              .optFoo.value().isEqualTo(TypeHolder.Foo.ONE)
              .optFoo.value(e -> e.isIn(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO));

  }
}
