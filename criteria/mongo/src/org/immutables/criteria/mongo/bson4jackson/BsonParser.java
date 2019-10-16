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

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import org.bson.AbstractBsonReader;
import org.bson.BsonReader;
import org.bson.BsonType;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Work horse of {@code bson4jackson} integration (reader).
 *
 * Smart conversion between {@link BsonType} and {@link JsonToken} plus caching and lazy loading of
 * parsed values. If you need to access BSON-only types directly
 * (like {@link org.bson.types.ObjectId} or {@link org.bson.types.Decimal128}) consider using Codecs on
 * the top of Jackson (see {@link JacksonCodecs} for more details).
 */
@NotThreadSafe
public class BsonParser extends ParserBase implements Wrapper<BsonReader> {

  private final AbstractBsonReader reader;

  private ParseContext context;
  /**
   * The ObjectCodec used to parse the Bson object(s)
   */
  private ObjectCodec _codec;

  BsonParser(IOContext ctxt, int jsonFeatures, AbstractBsonReader reader) {
    super(ctxt, jsonFeatures);
    this.reader = Objects.requireNonNull(reader, "reader");
    this.context = new ParseContext();
  }

  /**
   * Internal context to keep track of parsed values and field names
   */
  private static class ParseContext {
    private String fieldName;
    private Object value;
    private boolean skipValue;

    private String valueAsString() {
      return Objects.toString(value);
    }

    private void setValue(Object value) {
      this.value = value;
      skipValue = false;
    }

    /**
     * Means value was read from {@link BsonReader} this flag is used
     * on next read token iteration to checker wherever to call {@link BsonReader#skipValue()}.
     */
    private boolean hasValue() {
      return !skipValue;
    }

    /**
     * Reset state of current context
     */
    private void reset() {
      value = null;
      skipValue = true;
    }
  }

  @Override
  protected void _closeInput() throws IOException {
    if (isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE)) {
      reader.close();
    }
    context = null;
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
      return context.fieldName;
    }

    return null;
  }

  @Override
  public String getCurrentName() throws JsonParseException {
    if (state() == AbstractBsonReader.State.NAME) {
      return nextFieldName();
    } else if (state() == AbstractBsonReader.State.VALUE) {
      return context.fieldName;
    }

    return null;
  }

  @Override
  public Number getNumberValue() throws JsonParseException {
    if (currentToken() == JsonToken.VALUE_NULL) {
      // we know current value is null. throw exception because it can't be converted to number
      throw new JsonParseException(this, String.format("Can't convert %s (bson:%s) to %s", currentToken(), type(), Number.class.getName()));
    }

    if (!context.hasValue()) {
      // lazily read the value
      readValue();
    }

    if (context.value instanceof Number) {
      return (Number) context.value;
    }

    if (context.value instanceof String) {
      // should we really convert string to number ?
      return new BigDecimal((String) context.value);
    }

    String valueType = context.value != null ? context.value.getClass().getName() : "null";
    throw new JsonParseException(this, String.format("Can't convert %s (as %s) to %s", context.value, valueType, Number.class.getName()));

  }

  /**
   * Read (parse) current bson value and stored it in local {@link ParseContext} cache.
   */
  private void readValue() throws JsonParseException {
    final BsonType type = type();
    switch (type) {
      case DOUBLE:
        context.setValue(reader.readDouble());
        break;
      case INT32:
        context.setValue(reader.readInt32());
        break;
      case INT64:
        context.setValue(reader.readInt64());
        break;
      case DECIMAL128:
        context.setValue(reader.readDecimal128().bigDecimalValue());
        break;
      case DATE_TIME:
        context.setValue(reader.readDateTime());
        break;
      case TIMESTAMP:
        context.setValue(reader.readTimestamp().getValue());
        break;
      case SYMBOL:
        context.setValue(reader.readSymbol());
        break;
      case STRING:
        context.setValue(reader.readString());
        break;
      case OBJECT_ID:
        context.setValue(reader.readObjectId().toHexString());
        break;
      case REGULAR_EXPRESSION:
        context.setValue(reader.readRegularExpression().getPattern());
        break;
      case BOOLEAN:
        context.setValue(reader.readBoolean());
        break;
      case NULL:
        reader.readNull();
        context.setValue(null);
        break;
      case UNDEFINED:
        reader.readUndefined();
        context.setValue(null);
        break;
      case BINARY:
        context.setValue(reader.readBinaryData().getData());
        break;
      default:
        throw new JsonParseException(this, String.format("Unknown bson type %s (as json type %s)", type, currentToken()));
    }
  }

  @Override
  public BigInteger getBigIntegerValue() throws JsonParseException {
    Number number = getNumberValue();

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

    if (!context.hasValue() && state() == AbstractBsonReader.State.VALUE) {
      // means the value was not parsed before and can be skipped
      reader.skipValue();
    }

    context.reset();

    while (state() == AbstractBsonReader.State.TYPE) {
      reader.readBsonType();
    }

    switch (state()) {
      case INITIAL:
        reader.readStartDocument();
        return JsonToken.START_OBJECT;
      case NAME:
        context.fieldName = reader.readName();
        context.skipValue = false; // next token can be VALUE don't skip
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
        return readToken();
      default:
        throw new JsonParseException(this, String.format("Unexpected BSON state:%s type:%s", state(), type()));
    }
  }

  /**
   * Read next token in the stream
   */
  private JsonToken readToken() throws JsonParseException {
    switch (type()) {
      case END_OF_DOCUMENT:
        reader.readEndDocument();
        return JsonToken.END_OBJECT;
      case DOCUMENT:
        reader.readStartDocument();
        return JsonToken.START_OBJECT;
      case ARRAY:
        reader.readStartArray();
        return JsonToken.START_ARRAY;
      case BOOLEAN:
        final boolean value  = reader.readBoolean();
        context.setValue(value);
        return value ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE;
      case DATE_TIME:
      case TIMESTAMP:
        return JsonToken.VALUE_EMBEDDED_OBJECT;
      case NULL:
        reader.readNull();
        context.setValue(null);
        return JsonToken.VALUE_NULL;
      case SYMBOL:
      case STRING:
        return JsonToken.VALUE_STRING;
      case INT32:
      case INT64:
        return JsonToken.VALUE_NUMBER_INT;
      case DECIMAL128:
      case DOUBLE:
        return JsonToken.VALUE_NUMBER_FLOAT;
      case UNDEFINED:
        reader.readUndefined();
        context.setValue(null);
        return JsonToken.VALUE_NULL;
      case OBJECT_ID:
      case BINARY:
      case REGULAR_EXPRESSION:
      default:
        return JsonToken.VALUE_EMBEDDED_OBJECT;
    }
  }

  @Override
  public String getText() throws JsonParseException {
    final JsonToken token = currentToken();
    if (token == JsonToken.FIELD_NAME || type() == null) {
      // return current field name
      return context.fieldName;
    } else if (token == JsonToken.VALUE_NULL || token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
      return token.asString();
    }

    if (context.hasValue()) {
      return context.valueAsString();
    }

    readValue();
    return context.valueAsString();
  }

  @Override
  public byte[] getBinaryValue(Base64Variant variant) throws IOException {
    if (type() != BsonType.BINARY) {
      throw new JsonParseException(this, String.format("Can't read binary data. Expected type %s got %s", BsonType.BINARY, type()));
    }

    if (!context.hasValue()) {
      readValue();
    }

    return (byte[]) context.value;
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
