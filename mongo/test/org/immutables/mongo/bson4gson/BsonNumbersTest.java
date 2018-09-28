package org.immutables.mongo.bson4gson;

import com.google.gson.JsonObject;
import org.bson.BsonDocument;
import org.bson.BsonType;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.immutables.check.Checkers.check;

public class BsonNumbersTest {

  @Test
  public void basicNumbers() throws Exception {
    JsonObject obj = new JsonObject();
    obj.addProperty("boolean", true);
    obj.addProperty("byte", (byte) 1);
    obj.addProperty("short", (short) 4);
    obj.addProperty("int", Integer.MAX_VALUE);
    obj.addProperty("long", Long.MAX_VALUE);
    obj.addProperty("float", 55F);
    obj.addProperty("double", 128D);

    BsonDocument doc = Jsons.toBson(obj);

    check(doc.get("boolean").getBsonType()).is(BsonType.BOOLEAN);
    check(doc.get("boolean").asBoolean());
    check(doc.get("byte").getBsonType()).is(BsonType.INT32);
    check(doc.get("byte").asInt32().getValue()).is(1);
    check(doc.get("short").getBsonType()).is(BsonType.INT32);
    check(doc.get("short").asInt32().getValue()).is(4);
    check(doc.get("int").getBsonType()).is(BsonType.INT32);
    check(doc.get("int").asInt32().getValue()).is(Integer.MAX_VALUE);
    check(doc.get("long").getBsonType()).is(BsonType.INT64);
    check(doc.get("long").asInt64().getValue()).is(Long.MAX_VALUE);
    check(doc.get("float").getBsonType()).is(BsonType.DOUBLE);
    check(doc.get("float").asDouble().getValue()).is(55D);
    check(doc.get("double").getBsonType()).is(BsonType.DOUBLE);
    check(doc.get("double").asDouble().getValue()).is(128D);
  }

  @Test
  public void bigNumbers() throws Exception {
    JsonObject obj = new JsonObject();
    BigInteger bigInteger = new BigInteger(Long.toString(Long.MAX_VALUE)).multiply(new BigInteger("128"));
    obj.addProperty("bigInteger", bigInteger);
    BigDecimal bigDecimal = new BigDecimal(Long.MAX_VALUE).multiply(new BigDecimal(1024));
    obj.addProperty("bigDecimal", bigDecimal);

    BsonDocument bson = Jsons.toBson(obj);
    check(bson.get("bigInteger").getBsonType()).is(BsonType.DECIMAL128);
    check(bson.get("bigInteger").asDecimal128().decimal128Value().bigDecimalValue().toBigInteger()).is(bigInteger);
    check(bson.get("bigDecimal").getBsonType()).is(BsonType.DECIMAL128);
    check(bson.get("bigDecimal").asDecimal128().decimal128Value().bigDecimalValue()).is(bigDecimal);

    check(Jsons.toGson(bson)).is(obj);
  }
}
