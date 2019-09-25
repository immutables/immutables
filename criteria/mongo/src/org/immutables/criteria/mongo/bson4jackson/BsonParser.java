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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.google.common.base.Preconditions;
import org.bson.AbstractBsonReader;
import org.bson.BsonReader;
import org.bson.BsonType;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Delegates all streaming API to {@link BsonReader}
 */
@NotThreadSafe
public class BsonParser extends ParserBase implements Wrapper<BsonReader> {

  private final AbstractBsonReader reader;

  // cached value similar to _textBuffer to return same value on {@link getText()}
  private String _textValue;

  /**
   * The ObjectCodec used to parse the Bson object(s)
   */
  private ObjectCodec _codec;

  BsonParser(IOContext ctxt, int jsonFeatures, AbstractBsonReader reader) {
    super(ctxt, jsonFeatures);
    this.reader = Objects.requireNonNull(reader, "reader");
  }

  @Override
  protected void _closeInput() throws IOException {
    if (isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE)) {
      reader.close();
    }
    _closed = true;
  }

  @Override
  public ObjectCodec getCodec() {
    return _codec;
  }

  @Override
  public void setCodec(ObjectCodec codec) {
    this._codec = codec;
  }

  private AbstractBsonReader.State state() {
    return reader.getState();
  }

  @Override
  public String nextFieldName() throws JsonParseException {
    final JsonToken next = next();
    if (next == JsonToken.FIELD_NAME) {
      return reader.readName();
    } else if (next == JsonToken.START_OBJECT) {
      // advance if container type (object)
      return nextFieldName();
    }

    return null;
  }

  @Override
  public String getCurrentName() throws JsonParseException {
    if (state() == AbstractBsonReader.State.NAME) {
      return nextFieldName();
    } else if (state() == AbstractBsonReader.State.VALUE) {
      return reader.getCurrentName();
    }

    return null;
  }

  @Override
  public Number getNumberValue() throws JsonParseException {
    if (_numTypesValid != NR_UNKNOWN) {
      return cachedNumberValue();
    }

    final BsonType type = type();
    switch (type) {
      case DOUBLE:
        _numberDouble = reader.readDouble();
        _numTypesValid |= NR_DOUBLE;
        return _numberDouble;
      case INT32:
        _numberInt = reader.readInt32();
        _numTypesValid |= NR_INT;
        return _numberInt;
      case INT64:
        _numberLong = reader.readInt64();
        _numTypesValid |= NR_LONG;
        return _numberLong;
      case DECIMAL128:
        _numberBigDecimal = reader.readDecimal128().bigDecimalValue();
        _numTypesValid |= NR_BIGDECIMAL;
        return _numberBigDecimal;
      case STRING:
      case SYMBOL:
        // TODO should parser convert between string and numeric types ?
        _numberBigDecimal = new BigDecimal(type == BsonType.STRING ? reader.readString() : reader.readSymbol());
        _numTypesValid |= NR_BIGDECIMAL;
        return _numberBigDecimal;
      case DATE_TIME:
        _numberLong = reader.readDateTime();
        _numTypesValid |= NR_LONG;
        return _numberLong;
      case TIMESTAMP:
        _numberLong = reader.readTimestamp().getValue();
        _numTypesValid |= NR_LONG;
        return _numberLong;
      default:
        throw new JsonParseException(this, String.format("Can't convert bson type %s to %s (json type %s)", type, Number.class.getName(), currentToken()));
    }
  }

  private Number cachedNumberValue() {
    Preconditions.checkState(_numTypesValid != NR_UNKNOWN, "Number not cached. Expected state %s != %s", _numTypesValid, NR_UNKNOWN);
    if (currentToken() == JsonToken.VALUE_NUMBER_INT) {
      if ((_numTypesValid & NR_INT) != 0) {
        return _numberInt;
      }
      if ((_numTypesValid & NR_LONG) != 0) {
        return _numberLong;
      }
      if ((_numTypesValid & NR_BIGINT) != 0) {
        return _numberBigInt;
      }
      return _numberBigDecimal;
    }

    if ((_numTypesValid & NR_BIGDECIMAL) != 0) {
      return _numberBigDecimal;
    }
    return _numberDouble;
  }

  @Override
  public BigInteger getBigIntegerValue() throws JsonParseException {
    Number number = getNumberValue();
    if (number == null) {
      return null;
    }

    if (number instanceof BigInteger) {
      return (BigInteger) number;
    }

    if (number instanceof BigDecimal) {
      return ((BigDecimal) number).toBigInteger();
    }

    if (number instanceof Byte || number instanceof Integer || number instanceof Long || number instanceof Short) {
      return BigInteger.valueOf(number.longValue());
    } else if (number instanceof Double || number instanceof Float) {
      return BigDecimal.valueOf(number.doubleValue()).toBigInteger();
    }

    return new BigInteger(number.toString());
  }

  @Override
  public float getFloatValue() throws JsonParseException {
    return getNumberValue().floatValue();
  }

  @Override
  public double getDoubleValue() throws JsonParseException {
    return getNumberValue().doubleValue();
  }

  @Override
  public int getIntValue() throws JsonParseException {
    return getNumberValue().intValue();
  }

  @Override
  public long getLongValue() throws JsonParseException {
    return getNumberValue().longValue();
  }

  @Override
  public BigDecimal getDecimalValue() throws JsonParseException {
    Number number = getNumberValue();
    if (number == null) {
      return null;
    }

    if (number instanceof BigDecimal) {
      return (BigDecimal) number;
    }

    if (number instanceof BigInteger) {
      return new BigDecimal((BigInteger) number);
    }

    if (number instanceof Byte || number instanceof Integer ||
            number instanceof Long || number instanceof Short) {
      return BigDecimal.valueOf(number.longValue());
    } else if (number instanceof Double || number instanceof Float) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return new BigDecimal(number.toString());
  }

  private BsonType type() {
    return reader.getCurrentBsonType();
  }

  @Override
  public NumberType getNumberType() throws IOException {
    final BsonType type = type();
    switch (type) {
      case DOUBLE:
        return NumberType.DOUBLE;
      case INT32:
        return NumberType.INT;
      case INT64:
        return NumberType.LONG;
      case DECIMAL128:
        return NumberType.BIG_DECIMAL;
      default:
        throw new JsonParseException(this, String.format("Not a numeric type json:%s bson%s", currentToken(), type));
    }
  }

  @Override
  public JsonToken nextToken() throws JsonParseException {
    return _currToken = next();
  }

  private JsonToken next() throws JsonParseException {
    _numTypesValid = NR_UNKNOWN; // reset number caches
    _textValue = null;
    while (state() == AbstractBsonReader.State.TYPE) {
      reader.readBsonType();
    }

    switch (state()) {
      case INITIAL:
        reader.readStartDocument();
        return JsonToken.START_OBJECT;
      case NAME:
        return JsonToken.FIELD_NAME;
      case END_OF_DOCUMENT:
        reader.readEndDocument();
        return JsonToken.END_OBJECT;
      case END_OF_ARRAY:
        reader.readEndArray();
        return JsonToken.END_ARRAY;
      case DONE:
        return null;
      case VALUE:
        return toJsonToken(type());
      default:
        throw new JsonParseException(this, String.format("Unexpected BSON state:%s type:%s", state(), type()));
    }
  }

  private JsonToken toJsonToken(BsonType type) throws JsonParseException {
    switch (type) {
      case END_OF_DOCUMENT:
        reader.readEndDocument();
        return JsonToken.END_OBJECT;
      case DOCUMENT:
        reader.readStartDocument();
        return JsonToken.START_OBJECT;
      case ARRAY:
        reader.readStartArray();
        return JsonToken.START_ARRAY;
      case OBJECT_ID:
        return JsonToken.VALUE_EMBEDDED_OBJECT;
      case BOOLEAN:
        final boolean value  = reader.readBoolean();
        return value ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE;
      case DATE_TIME:
      case TIMESTAMP:
        return JsonToken.VALUE_EMBEDDED_OBJECT;
      case NULL:
        reader.readNull();
        return JsonToken.VALUE_NULL;
      case REGULAR_EXPRESSION:
        return JsonToken.VALUE_EMBEDDED_OBJECT;
      case SYMBOL:
      case STRING:
        return JsonToken.VALUE_STRING;
      case INT32:
      case INT64:
        return JsonToken.VALUE_NUMBER_INT;
      case DECIMAL128:
        return JsonToken.VALUE_NUMBER_FLOAT;
      case DOUBLE:
        return JsonToken.VALUE_NUMBER_FLOAT;
      case BINARY:
        return JsonToken.VALUE_EMBEDDED_OBJECT;
      default:
        throw new JsonParseException(this, String.format("Unknown BSON type:%s", type));
    }
  }

  @Override
  public String getText() throws JsonParseException {
    if (_textValue != null) {
      return _textValue;
    }

    final BsonType type = type();
    if (type == BsonType.SYMBOL) {
      _textValue = reader.readSymbol();
      return _textValue;
    }
    if (type == BsonType.STRING) {
      _textValue = reader.readString();
      return _textValue;
    }
    if (type == BsonType.OBJECT_ID) {
      _textValue = reader.readObjectId().toHexString();
      return _textValue;
    }

    if (type == BsonType.REGULAR_EXPRESSION) {
      _textValue = reader.readRegularExpression().getPattern();
      return _textValue;
    }

    final JsonToken token = currentToken();
    if (token == JsonToken.FIELD_NAME) {
      // return current field name
      return reader.getCurrentName();
    }

    // number compatible types
    if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT ||
            type == BsonType.DATE_TIME || type == BsonType.TIMESTAMP) {
      return getNumberValue().toString();
    }

    return token != null ? token.asString() : null;
  }

  @Override
  public char[] getTextCharacters() throws IOException {
    return getText().toCharArray();
  }

  @Override
  public int getTextLength() throws IOException {
    return getText().length();
  }

  @Override
  public int getTextOffset() throws IOException {
    return 0;
  }

  @Override
  public boolean hasTextCharacters() {
    return false;
  }

  @Override
  public BsonReader unwrap() {
    return reader;
  }
}
