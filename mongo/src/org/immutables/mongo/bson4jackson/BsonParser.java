/*
   Copyright 2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.mongo.bson4jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.google.common.base.Preconditions;
import org.bson.AbstractBsonReader;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.immutables.mongo.Wrapper;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Delegates all streaming API to {@link BsonReader}
 */
@NotThreadSafe
public class BsonParser extends ParserBase implements Wrapper<BsonReader> {

  private final AbstractBsonReader reader;

  /**
   * The ObjectCodec used to parse the Bson object(s)
   */
  private ObjectCodec _codec;


  BsonParser(IOContext ctxt, int jsonFeatures, AbstractBsonReader reader) {
    super(ctxt, jsonFeatures);
    this.reader = Preconditions.checkNotNull(reader, "reader");
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
  public String nextFieldName() throws IOException {
    if (next() == JsonToken.FIELD_NAME) {
      return reader.readName();
    }

    return null;
  }

  @Override
  public String getCurrentName() throws IOException {
    if (state() == AbstractBsonReader.State.NAME) {
      return nextFieldName();
    } else if (state() == AbstractBsonReader.State.VALUE) {
      return reader.getCurrentName();
    }

    return null;
  }

  @Override
  public Number getNumberValue() throws IOException {
    final BsonType type = type();
    switch (type) {
      case DOUBLE:
        return reader.readDouble();
      case INT32:
        return reader.readInt32();
      case INT64:
        return reader.readInt64();
      case DECIMAL128:
        return reader.readDecimal128().bigDecimalValue();
      case STRING:
        return new BigDecimal(reader.readString());
    }

    throw new IllegalStateException(String.format("Can't convert %s to %s", type, Number.class.getName()));
  }

  @Override
  public BigInteger getBigIntegerValue() throws IOException {
    final Number value = getNumberValue();

    if (value instanceof BigInteger) {
      return (BigInteger) value;
    } else if (value instanceof BigDecimal) {
      return ((BigDecimal) value).toBigInteger();
    }

    return BigInteger.valueOf(value.longValue());
  }

  @Override
  public float getFloatValue() throws IOException {
    return getNumberValue().floatValue();
  }

  @Override
  public double getDoubleValue() throws IOException {
    return getNumberValue().doubleValue();
  }

  @Override
  public int getIntValue() throws IOException {
    return getNumberValue().intValue();
  }

  @Override
  public long getLongValue() throws IOException {
    return getNumberValue().longValue();
  }

  @Override
  public BigDecimal getDecimalValue() throws IOException {
    final BsonType type = type();
    switch (type) {
      case DOUBLE:
        return BigDecimal.valueOf(getNumberValue().doubleValue());
      case INT32:
        return new BigDecimal(getNumberValue().intValue());
      case INT64:
        return BigDecimal.valueOf(getNumberValue().longValue());
      case DECIMAL128:
      case STRING:
        return (BigDecimal) getNumberValue();
    }

    throw new IllegalStateException(String.format("Can't convert %s to %s", type, BigDecimal.class.getName()));
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
    }

    throw new IllegalStateException(String.format("Not a number type %s", type));
  }

  @Override
  public JsonToken nextToken() throws IOException {
    return _currToken = next();
  }

  private JsonToken next() throws IOException {
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
    }

    throw new IllegalStateException(String.format("Unexpected state: %s currentType: %s", state(), type()));
  }

  private JsonToken toJsonToken(BsonType type) {
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
    }

    throw new IllegalStateException(String.format("Unknown type %s", type));
  }

  @Override
  public String getText() throws IOException {
    final BsonType type = type();
    if (type == BsonType.SYMBOL) {
      return reader.readSymbol();
    } else if (type == BsonType.STRING) {
      return reader.readString();
    }

    throw new IllegalStateException(String.format("Bad BSON type: %s expected String or Symbol", type));
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
    throw new UnsupportedOperationException();
  }

  @Override
  public BsonReader unwrap() {
    return reader;
  }
}
