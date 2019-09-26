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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.bson.BsonBinary;
import org.bson.BsonBoolean;
import org.bson.BsonDateTime;
import org.bson.BsonDecimal128;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.BsonUndefined;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
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
    check(Parsers.parserAt(value).currentToken()).is(JsonToken.VALUE_NUMBER_INT);
    check(Parsers.parserAt(value).getNumberType()).is(JsonParser.NumberType.INT);
    check(Parsers.parserAt(value).getIntValue()).is(42);
    check(Parsers.parserAt(value).getLongValue()).is(42L);
    check(Parsers.parserAt(value).getDoubleValue()).is(42D);
    check(Parsers.parserAt(value).getDecimalValue()).is(BigDecimal.valueOf(42));
    check(Parsers.parserAt(value).getBigIntegerValue()).is(BigInteger.valueOf(42));
    check(Parsers.parserAt(value).getNumberValue()).is(42);
  }

  @Test
  void int64() throws IOException {
    final BsonInt32 value = new BsonInt32(42);
    check(Parsers.parserAt(value).currentToken()).is(JsonToken.VALUE_NUMBER_INT);
    check(Parsers.parserAt(new BsonInt64(64)).getNumberType()).is(JsonParser.NumberType.LONG);

    check(Parsers.parserAt(new BsonInt64(64)).getIntValue()).is(64);
    check(Parsers.parserAt(new BsonInt64(64)).getLongValue()).is(64L);
    check(Parsers.parserAt(new BsonInt64(64)).getDoubleValue()).is(64D);
    check(Parsers.parserAt(new BsonInt64(64)).getDecimalValue()).is(BigDecimal.valueOf(64));
    check(Parsers.parserAt(new BsonInt64(64)).getBigIntegerValue()).is(BigInteger.valueOf(64));
    check(Parsers.parserAt(new BsonInt64(64)).getNumberValue()).is(64L);
  }

  @Test
  void bsonDouble() throws IOException {
    final BsonDouble value = new BsonDouble(1.1);
    check(Parsers.parserAt(value).currentToken()).is(JsonToken.VALUE_NUMBER_FLOAT);
    check(Parsers.parserAt(value).getIntValue()).is(1);
    check(Parsers.parserAt(value).getLongValue()).is(1L);
    check(Parsers.parserAt(value).getDoubleValue()).is(1.1D);
    check(Parsers.parserAt(value).getDecimalValue()).is(BigDecimal.valueOf(1.1));
    check(Parsers.parserAt(value).getBigIntegerValue()).is(BigDecimal.valueOf(1.1).toBigInteger());
    check(Parsers.parserAt(value).getNumberValue()).is(1.1D);
  }

  @Test
  void dateTime() throws IOException {
    final long epoch = System.currentTimeMillis();
    final BsonDateTime value = new BsonDateTime(epoch);
    check(Parsers.parserAt(value).getIntValue()).is((int) epoch);
    check(Parsers.parserAt(value).getLongValue()).is(epoch);
    check(Parsers.parserAt(value).getDoubleValue()).is((double) epoch);
    check(Parsers.parserAt(value).getDecimalValue()).is(BigDecimal.valueOf(epoch));
    check(Parsers.parserAt(value).getBigIntegerValue()).is(BigInteger.valueOf(epoch));
    check(Parsers.parserAt(value).getText()).is(Long.toString(epoch));
  }

  @Test
  void timestamp() throws IOException {
    final long epoch = System.currentTimeMillis();
    check(Parsers.parserAt(new BsonTimestamp(epoch)).getIntValue()).is((int) epoch);
    check(Parsers.parserAt(new BsonTimestamp(epoch)).getLongValue()).is(epoch);
    check(Parsers.parserAt(new BsonTimestamp(epoch)).getDoubleValue()).is((double) epoch);
    check(Parsers.parserAt(new BsonTimestamp(epoch)).getDecimalValue()).is(BigDecimal.valueOf(epoch));
    check(Parsers.parserAt(new BsonTimestamp(epoch)).getBigIntegerValue()).is(BigInteger.valueOf(epoch));
    check(Parsers.parserAt(new BsonTimestamp(epoch)).getText()).is(Long.toString(epoch));
  }

  @Test
  void regexpPattern() throws IOException {
    check(Parsers.parserAt(new BsonRegularExpression("abc")).getText()).is("abc");
    check(Parsers.parserAt(new BsonRegularExpression(".*")).getText()).is(".*");
  }

  @Test
  void decimal128() throws IOException {
    BsonDecimal128 value = new BsonDecimal128(new Decimal128(BigDecimal.valueOf(1.1)));
    check(Parsers.parserAt(value).getCurrentToken()).is(JsonToken.VALUE_NUMBER_FLOAT);
    check(Parsers.parserAt(value).getNumberType()).is(JsonParser.NumberType.BIG_DECIMAL);
    check(Parsers.parserAt(value).getDoubleValue()).is(1.1);
    check(Parsers.parserAt(value).getDecimalValue()).is(BigDecimal.valueOf(1.1));
  }

  @Test
  void binary() throws IOException {
    byte[] data = {1, 2, 3};
    JsonParser parser = Parsers.parserAt(new BsonBinary(data));
    check(parser.getCurrentToken()).is(JsonToken.VALUE_EMBEDDED_OBJECT);
    check(parser.getBinaryValue()).isOf((byte) 1, (byte) 2, (byte) 3);
    check(parser.getBinaryValue()).isOf((byte) 1, (byte) 2, (byte) 3);

    Assertions.assertThrows(JsonParseException.class, parser::getNumberType);
    Assertions.assertThrows(JsonParseException.class, parser::getBooleanValue);
  }

  @Test
  void parseExceptions() {
    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(new BsonBoolean(true)).getNumberValue();
    });

    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(new BsonBoolean(true)).getLongValue();
    });

    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(BsonNull.VALUE).getNumberValue();
    });

    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(BsonNull.VALUE).getNumberType();
    });

    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(new BsonInt32(42)).getBooleanValue();
    });

    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(new BsonBoolean(true)).getNumberType();
    });

    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(new BsonDateTime(42)).getBooleanValue();
    });

    Assertions.assertThrows(JsonParseException.class, () -> {
      Parsers.parserAt(new BsonTimestamp(42)).getBooleanValue();
    });
  }

  @Test
  void objectId() throws IOException {
    ObjectId id = ObjectId.get();
    check(Parsers.parserAt(new BsonObjectId(id)).getText()).is(id.toHexString());
  }

  @Test
  void undefined() throws IOException {
    JsonParser parser = Parsers.parserAt(new BsonUndefined());
    check(parser.currentToken()).is(JsonToken.VALUE_NULL);
    check(parser.getText()).is(JsonToken.VALUE_NULL.asString());
    parser.nextToken();
  }

  @Test
  void nullValue() throws IOException {
    check(Parsers.parserAt(BsonNull.VALUE).getCurrentToken()).is(JsonToken.VALUE_NULL);
    check(Parsers.parserAt(BsonNull.VALUE).getText()).is(JsonToken.VALUE_NULL.asString());
  }

  @Test
  void booleanValue() throws IOException {
    check(Parsers.parserAt(new BsonBoolean(true)).getText()).is("true");
    check(Parsers.parserAt(new BsonBoolean(false)).getText()).is("false");
    check(Parsers.parserAt(new BsonBoolean(true)).getBooleanValue());
  }

}
