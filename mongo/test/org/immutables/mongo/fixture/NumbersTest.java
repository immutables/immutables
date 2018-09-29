package org.immutables.mongo.fixture;

import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;

/**
 * A set of tests for entities with primitive numbers (byte, short, int, long ...) and more advanced number wrappers
 * (BigInteger, BigDecimal, AtomicLong, AtomicInteger etc.).
 *
 * Some conversions are provided by Gson directly (like {@link com.google.gson.internal.bind.TypeAdapters#ATOMIC_INTEGER}
 */
@Gson.TypeAdapters
public class NumbersTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  @Test
  public void primitives() {
    final PrimitivesRepository repo = new PrimitivesRepository(context.setup());
    ObjectId id = ObjectId.get();
    ImmutablePrimitives doc = ImmutablePrimitives.builder()
            .id(id)
            .booleanValue(true)
            .byteValue((byte) 0)
            .shortValue((short) 12)
            .intValue(42)
            .longValue(123L)
            .floatValue(11.11F)
            .doubleValue(22.22D)
            .build();

    repo.insert(doc).getUnchecked();
    check(repo.findById(id).fetchAll().getUnchecked()).hasContentInAnyOrder(doc);
  }

  @Test
  public void boxed() {
    final BoxedRepository repo = new BoxedRepository(context.setup());
    ObjectId id = ObjectId.get();

    Boxed doc = ImmutableBoxed.builder()
            .id(id)
            .booleanValue(Boolean.FALSE)
            .byteValue(Byte.MAX_VALUE)
            .shortValue(Short.MAX_VALUE)
            .intValue(Integer.MAX_VALUE)
            .longValue(Long.MAX_VALUE)
            .floatValue(Float.MAX_VALUE)
            .doubleValue(Double.MAX_VALUE)
            .build();

    repo.insert(doc).getUnchecked();
    check(repo.findById(id).fetchAll().getUnchecked()).hasContentInAnyOrder(doc);
  }

  @Test
  public void advanced() {
    final AdvancedRepository repo = new AdvancedRepository(context.setup());
    ObjectId id = ObjectId.get();

    Advanced doc = ImmutableAdvanced.builder()
            .id(id)
            .bigDecimal(new BigDecimal(Long.MAX_VALUE).multiply(new BigDecimal(128))) // make number big
            .bigInteger(new BigInteger(String.valueOf(Long.MAX_VALUE)).multiply(new BigInteger("128"))) // make it also big
            .atomicBoolean(new AtomicBoolean())
            .atomicInteger(new AtomicInteger(55))
            .atomicLong(new AtomicLong(77))
            .build();

    repo.insert(doc).getUnchecked();

    final Advanced doc2 = repo.findById(id).fetchFirst().getUnchecked().get();
    check(doc2.id()).is(id);

    check(doc2.bigDecimal()).is(doc.bigDecimal());
    check(doc2.bigInteger()).is(doc.bigInteger());
    check(!doc2.atomicBoolean().get());
    check(doc2.atomicInteger().get()).is(doc.atomicInteger().get());
    check(doc2.atomicLong().get()).is(doc.atomicLong().get());
  }

  /**
   * Serializing big numbers which can't be stored in {@link org.bson.types.Decimal128} format.
   * They should be serialized as strings (or binary) in mongo.
   */
  @Test
  public void larger_than_decimal128() {
    final AdvancedRepository repo = new AdvancedRepository(context.setup());
    final ObjectId id = ObjectId.get();
    final BigInteger integer = BigInteger.valueOf(Long.MAX_VALUE).pow(4);

    try {
      new Decimal128(new BigDecimal(integer));
      fail("Should fail for " + integer);
    } catch (NumberFormatException ignore) {
      // expected
    }

    final Advanced doc = ImmutableAdvanced.builder()
            .id(id)
            .bigDecimal(new BigDecimal(integer)) // make number big
            .bigInteger(integer) // make it also big
            .atomicBoolean(new AtomicBoolean(false))
            .atomicInteger(new AtomicInteger(1))
            .atomicLong(new AtomicLong(2))
            .build();

    repo.insert(doc).getUnchecked();

    final Advanced doc2 = repo.findById(id).fetchFirst().getUnchecked().get();
    check(doc2.bigDecimal().unscaledValue()).is(integer);
    check(doc2.bigInteger()).is(integer);

  }

  @Mongo.Repository
  @Value.Immutable
  interface Primitives {

    @Mongo.Id
    ObjectId id();

    boolean booleanValue();

    byte byteValue();

    short shortValue();

    int intValue();

    long longValue();

    float floatValue();

    double doubleValue();
  }

  @Mongo.Repository
  @Value.Immutable
  interface Boxed {

    @Mongo.Id
    ObjectId id();

    Boolean booleanValue();

    Byte byteValue();

    Short shortValue();

    Integer intValue();

    Long longValue();

    Float floatValue();

    Double doubleValue();
  }

  @Mongo.Repository
  @Value.Immutable
  interface Advanced {

    @Mongo.Id
    ObjectId id();

    BigDecimal bigDecimal();

    BigInteger bigInteger();

    AtomicBoolean atomicBoolean();

    AtomicInteger atomicInteger();

    AtomicLong atomicLong();
  }

}
