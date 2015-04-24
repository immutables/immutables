/*
    Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.gson.stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Callable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * {@link JsonWriter} impementation backed by Jackson's {@link JsonGenerator}.
 * Provides measurable JSON writing improvements over Gson's native implementation.
 * Error reporting is might differ, however.
 */
@NotThreadSafe
public class JsonGeneratorWriter extends JsonWriter implements Callable<JsonGenerator> {
  private static final Writer UNSUPPORTED_WRITER = new Writer() {
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void flush() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
      throw new UnsupportedOperationException();
    }
  };

  private final JsonGenerator generator;

  public JsonGeneratorWriter(JsonGenerator generator) {
    super(UNSUPPORTED_WRITER);
    this.generator = generator;
  }

  public JsonGenerator getGenerator() {
    return generator;
  }

  @Override
  public JsonWriter beginArray() throws IOException {
    generator.writeStartArray();
    return this;
  }

  @Override
  public JsonWriter endArray() throws IOException {
    generator.writeEndArray();
    return this;
  }

  @Override
  public JsonWriter beginObject() throws IOException {
    generator.writeStartObject();
    return this;
  }

  @Override
  public JsonWriter endObject() throws IOException {
    generator.writeEndObject();
    return this;
  }

  @Override
  public JsonWriter name(String name) throws IOException {
    generator.writeFieldName(name);
    return this;
  }

  @Override
  public JsonWriter value(String value) throws IOException {
    generator.writeString(value);
    return this;
  }

  @Override
  public JsonWriter nullValue() throws IOException {
    generator.writeNull();
    return this;
  }

  @Override
  public JsonWriter value(boolean value) throws IOException {
    generator.writeBoolean(value);
    return this;
  }

  @Override
  public JsonWriter value(double value) throws IOException {
    if (!isLenient() && (Double.isNaN(value) || Double.isInfinite(value))) {
      throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
    }
    generator.writeNumber(value);
    return this;
  }

  @Override
  public JsonWriter value(long value) throws IOException {
    generator.writeNumber(value);
    return this;
  }

  @Override
  public JsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    double d = value.doubleValue();
    if (!isLenient()) {
      if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
      }
    }
    generator.writeNumber(d);
    return this;
  }

  @Override
  public void flush() throws IOException {
    generator.flush();
  }

  @Override
  public void close() throws IOException {
    generator.close();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + generator + ")";
  }

  public String[] getLocationInfo() {
    return new String[] {"path " + getPath()};
  }

  /**
   * Implements {@link Callable} mostly as a marker interface.
   * Better use {@link #getGenerator()} to get generator.
   * @return unwrapped {@link JsonGenerator}
   */
  @Override
  public JsonGenerator call() throws Exception {
    return generator;
  }

  public String getPath() {
    return JsonParserReader.toJsonPath(generator.getOutputContext());
  }
}
