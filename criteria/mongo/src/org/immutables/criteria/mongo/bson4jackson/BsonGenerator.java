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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import org.bson.BsonWriter;
import org.bson.types.Decimal128;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Delegates all streaming API to {@link BsonWriter}.
 */
@NotThreadSafe
public class BsonGenerator extends GeneratorBase implements Wrapper<BsonWriter> {

  private final BsonWriter writer;

  BsonGenerator(int jsonFeatures, ObjectCodec codec, BsonWriter writer) {
    super(jsonFeatures, codec);
    this.writer = Objects.requireNonNull(writer, "writer");
  }

  @Override
  public void writeStartArray() throws IOException {
    writer.writeStartArray();
  }

  @Override
  public void writeEndArray() throws IOException {
    writer.writeEndArray();
  }

  @Override
  public void writeStartObject() throws IOException {
    writer.writeStartDocument();
  }

  @Override
  public void writeEndObject() throws IOException {
    writer.writeEndDocument();
  }

  @Override
  public void writeFieldName(String name) throws IOException {
    writer.writeName(name);
  }

  @Override
  public void writeString(String text) throws IOException {
    writer.writeString(text);
  }

  @Override
  public void writeString(char[] text, int offset, int len) throws IOException {
    writer.writeString(new String(text, offset, len));
  }

  @Override
  public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
    writer.writeString(new String(text, offset, length));
  }

  @Override
  public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
    writer.writeString(new String(text, offset, length));
  }

  @Override
  public void writeRaw(String text) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(String text, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(char[] text, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(char c) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeNumber(int number) throws IOException {
    writer.writeInt32(number);
  }

  @Override
  public void writeNumber(long number) throws IOException {
    writer.writeInt64(number);
  }

  @Override
  public void writeNumber(BigInteger number) throws IOException {
    writeNumber(new BigDecimal(number));
  }

  @Override
  public void writeNumber(double number) throws IOException {
    writer.writeDouble(number);
  }

  @Override
  public void writeNumber(float number) throws IOException {
    writer.writeDouble(number);
  }

  @Override
  public void writeNumber(BigDecimal number) throws IOException {
    try {
      writer.writeDecimal128(new Decimal128(number));
    } catch (NumberFormatException e) {
      writer.writeString(number.toString());
    }
  }

  @Override
  public void writeNumber(String encodedValue) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBoolean(boolean state) throws IOException {
    writer.writeBoolean(state);
  }

  @Override
  public void writeNull() throws IOException {
    writer.writeNull();
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  protected void _releaseBuffers() {

  }

  @Override
  protected void _verifyValueWrite(String typeMsg) throws IOException {

  }

  @Override
  public BsonWriter unwrap() {
    return writer;
  }
}
