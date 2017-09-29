package org.immutables.mongo.repository.internal;

import com.google.gson.internal.LazilyParsedNumber;
import org.bson.BsonBinary;
import org.bson.BsonRegularExpression;
import org.bson.types.ObjectId;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class BsonWriter extends com.google.gson.stream.JsonWriter {
  private static final Writer UNWRITABLE_WRITER = new Writer() {
    @Override public void write(char[] buffer, int offset, int counter) {
      throw new AssertionError();
    }
    @Override public void flush() throws IOException {
      throw new AssertionError();
    }
    @Override public void close() throws IOException {
      throw new AssertionError();
    }
  };

  private final org.bson.BsonWriter delegate;

  BsonWriter(org.bson.BsonWriter delegate) {
    super(UNWRITABLE_WRITER);
    this.delegate = checkNotNull(delegate, "delegate");
  }

  @Override
  public com.google.gson.stream.JsonWriter beginArray() throws IOException {
    delegate.writeStartArray();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter endArray() throws IOException {
    delegate.writeEndArray();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter beginObject() throws IOException {
    delegate.writeStartDocument();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter endObject() throws IOException {
    delegate.writeEndDocument();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter name(String name) throws IOException {
    delegate.writeName(name);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(String value) throws IOException {
    delegate.writeString(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter jsonValue(String value) throws IOException {
    throw new UnsupportedOperationException("Can't write directly JSON to BSON");
  }

  @Override
  public com.google.gson.stream.JsonWriter nullValue() throws IOException {
    delegate.writeNull();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(boolean value) throws IOException {
    delegate.writeBoolean(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(Boolean value) throws IOException {
    if (value == null) {
      delegate.writeNull();
    } else {
      delegate.writeBoolean(value);
    }

    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(double value) throws IOException {
    delegate.writeDouble(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(long value) throws IOException {
    delegate.writeInt64(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    } else if (value instanceof Double) {
      return value(value.doubleValue());
    } else if (value instanceof Float) {
      return value(value.floatValue());
    } else if (value instanceof Long){
      return value(value.longValue());
    } else if (value instanceof Integer) {
      return value(value.intValue());
    } else if (value instanceof Short) {
      return value((int) value.shortValue());
    } else if (value instanceof LazilyParsedNumber) {
      return value(value.longValue());
    } else {
      throw new UnsupportedOperationException(String.format("Don't know how to write %s: %s", value.getClass().getName(), value));
    }
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  @Override
  public void close() throws IOException {
    if (delegate instanceof Closeable) {
      ((Closeable) delegate).close();
    }
  }

  public void valueBinary(byte[] data) {
    delegate.writeBinaryData(new BsonBinary(data));
  }

  public void valueObjectId(byte[] data) {
    delegate.writeObjectId(new ObjectId(data));
  }

  public void value(Pattern pattern) {
    delegate.writeRegularExpression(new BsonRegularExpression(pattern.pattern()));
  }

  public void valueTimeInstant(long value) {
    delegate.writeDateTime(value);
  }


}
