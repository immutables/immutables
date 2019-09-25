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

package org.immutables.criteria.mongo.bson4jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.bson.BsonBoolean;
import org.bson.BsonDateTime;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.BsonValue;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.immutables.check.Checkers.check;

/**
 * Validate different type conversions like {@code DateTime => Long => String}
 */
class TypeConversionTest {

  @Test
  void int32() throws IOException {
    final BsonInt32 value = new BsonInt32(42);
    check(parserFor(value).currentToken()).is(JsonToken.VALUE_NUMBER_INT);
    check(parserFor(value).getNumberType()).is(JsonParser.NumberType.INT);
    check(parserFor(value).getIntValue()).is(42);
    check(parserFor(value).getLongValue()).is(42L);
    check(parserFor(value).getDoubleValue()).is(42D);
    check(parserFor(value).getDecimalValue()).is(BigDecimal.valueOf(42));
    check(parserFor(value).getBigIntegerValue()).is(BigInteger.valueOf(42));
    check(parserFor(value).getNumberValue()).is(42);
  }

  @Test
  void int64() throws IOException {
    final BsonInt32 value = new BsonInt32(42);
    check(parserFor(value).currentToken()).is(JsonToken.VALUE_NUMBER_INT);
    check(parserFor(new BsonInt64(64)).getNumberType()).is(JsonParser.NumberType.LONG);

    check(parserFor(new BsonInt64(64)).getIntValue()).is(64);
    check(parserFor(new BsonInt64(64)).getLongValue()).is(64L);
    check(parserFor(new BsonInt64(64)).getDoubleValue()).is(64D);
    check(parserFor(new BsonInt64(64)).getDecimalValue()).is(BigDecimal.valueOf(64));
    check(parserFor(new BsonInt64(64)).getBigIntegerValue()).is(BigInteger.valueOf(64));
    check(parserFor(new BsonInt64(64)).getNumberValue()).is(64L);
  }

  @Test
  void bsonDouble() throws IOException {
    final BsonDouble value = new BsonDouble(1.1);
    check(parserFor(value).currentToken()).is(JsonToken.VALUE_NUMBER_FLOAT);
    check(parserFor(value).getIntValue()).is(1);
    check(parserFor(value).getLongValue()).is(1L);
    check(parserFor(value).getDoubleValue()).is(1.1D);
    check(parserFor(value).getDecimalValue()).is(BigDecimal.valueOf(1.1));
    check(parserFor(value).getBigIntegerValue()).is(BigDecimal.valueOf(1.1).toBigInteger());
    check(parserFor(value).getNumberValue()).is(1.1D);
  }

  @Test
  void dateTime() throws IOException {
    final long epoch = System.currentTimeMillis();
    final BsonDateTime value = new BsonDateTime(epoch);
    check(parserFor(value).getIntValue()).is((int) epoch);
    check(parserFor(value).getLongValue()).is(epoch);
    check(parserFor(value).getDoubleValue()).is((double) epoch);
    check(parserFor(value).getDecimalValue()).is(BigDecimal.valueOf(epoch));
    check(parserFor(value).getBigIntegerValue()).is(BigInteger.valueOf(epoch));
    check(parserFor(value).getText()).is(Long.toString(epoch));
  }

  @Test
  void timestamp() throws IOException {
    final long epoch = System.currentTimeMillis();
    check(parserFor(new BsonTimestamp(epoch)).getIntValue()).is((int) epoch);
    check(parserFor(new BsonTimestamp(epoch)).getLongValue()).is(epoch);
    check(parserFor(new BsonTimestamp(epoch)).getDoubleValue()).is((double) epoch);
    check(parserFor(new BsonTimestamp(epoch)).getDecimalValue()).is(BigDecimal.valueOf(epoch));
    check(parserFor(new BsonTimestamp(epoch)).getBigIntegerValue()).is(BigInteger.valueOf(epoch));
    check(parserFor(new BsonTimestamp(epoch)).getText()).is(Long.toString(epoch));
  }

  @Test
  void regexpPattern() throws IOException {
    check(parserFor(new BsonRegularExpression("abc")).getText()).is("abc");
    check(parserFor(new BsonRegularExpression(".*")).getText()).is(".*");
  }

  @Test
  void decimal128() throws IOException {
    BsonDecimal128 value = new BsonDecimal128(new Decimal128(BigDecimal.valueOf(1.1)));
    check(parserFor(value).getCurrentToken()).is(JsonToken.VALUE_NUMBER_FLOAT);
    check(parserFor(value).getNumberType()).is(JsonParser.NumberType.BIG_DECIMAL);
    check(parserFor(value).getDoubleValue()).is(1.1);
    check(parserFor(value).getDecimalValue()).is(BigDecimal.valueOf(1.1));
  }

  @Test
  void objectId() throws IOException {
    ObjectId id = ObjectId.get();
    check(parserFor(new BsonObjectId(id)).getText()).is(id.toHexString());
  }

  @Test
  void nullValue() throws IOException {
    check(parserFor(BsonNull.VALUE).getCurrentToken()).is(JsonToken.VALUE_NULL);
    check(parserFor(BsonNull.VALUE).getText()).is(JsonToken.VALUE_NULL.asString());
  }

  @Test
  void booleanValue() throws IOException {
    check(parserFor(new BsonBoolean(true)).getText()).is("true");
    check(parserFor(new BsonBoolean(false)).getText()).is("false");
    check(parserFor(new BsonBoolean(true)).getBooleanValue());
  }

  private static JsonParser parserFor(BsonValue value) throws IOException {
    JsonParser parser = Parsers.createParser(new BsonDocument("value", value));
    parser.nextToken();
    check(parser.nextFieldName()).is("value");
    parser.nextToken();
    return parser;
  }
}
