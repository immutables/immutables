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

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;
import org.bson.AbstractBsonReader;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Registry based on {@link ObjectMapper}
 */
class JacksonCodecRegistry implements CodecRegistry {

  private final ObjectMapper mapper;

  private JacksonCodecRegistry(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  public static JacksonCodecRegistry of(ObjectMapper mapper) {
    return new JacksonCodecRegistry(mapper);
  }

  @Override
  public <T> Codec<T> get(Class<T> clazz) {
    final JavaType javaType = mapper.getTypeFactory().constructType(clazz);
    if (!(mapper.canSerialize(clazz) && mapper.canDeserialize(javaType))) {
      throw new CodecConfigurationException(String.format("%s (javaType: %s) not supported by Jackson Mapper", clazz, javaType));
    }

    return new JacksonCodec<>(clazz, mapper);
  }

  @Override
  public <T> Codec<T> get(Class<T> clazz, CodecRegistry codecRegistry) {
    return LegacyCodecAdapter.of(this).get(clazz, codecRegistry);
  }

  private static class JacksonCodec<T> implements Codec<T> {

    private final Class<T> clazz;
    private final ObjectReader reader;
    private final ObjectWriter writer;
    private final IOContext ioContext;

    JacksonCodec(Class<T> clazz, ObjectMapper mapper) {
      this.clazz = Objects.requireNonNull(clazz, "clazz");
      Objects.requireNonNull(mapper, "mapper");
      this.reader = mapper.readerFor(clazz);
      this.writer = mapper.writerFor(clazz);
      this.ioContext =  new IOContext(new BufferRecycler(), null, false);
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
      Preconditions.checkArgument(reader instanceof AbstractBsonReader,
              "Expected reader to be %s for %s but was %s",
              AbstractBsonReader.class.getName(), clazz, reader.getClass());
      final BsonParser parser = new BsonParser(ioContext, 0, (AbstractBsonReader) reader);
      try {
        return this.reader.readValue(parser);
      } catch (IOException e) {
        throw new UncheckedIOException("Error while decoding " + clazz, e);
      }
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
      final BsonGenerator generator = new BsonGenerator(0, writer);
      try {
        this.writer.writeValue(generator, value);
      } catch (IOException e) {
        throw new UncheckedIOException("Couldn't serialize [" + value + "] as " + getEncoderClass(), e);
      }
    }

    @Override
    public Class<T> getEncoderClass() {
      return clazz;
    }
  }
}
