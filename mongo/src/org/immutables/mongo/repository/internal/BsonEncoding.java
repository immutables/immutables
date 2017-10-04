/*
   Copyright 2013-2015 Immutables Authors and Contributors

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
package org.immutables.mongo.repository.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.gson.TypeAdapter;
import java.io.IOException;
import org.bson.AbstractBsonReader;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.Decoder;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * MongoDB (Bson) driver specific codecs and helper methods. Utility class.
 */
public final class BsonEncoding {

  private BsonEncoding() {}

  /**
   * "Smart" registry just for this particular {@code type}. It is typically composed with existing
   * registries using {@link org.bson.codecs.configuration.CodecRegistries#fromRegistries(CodecRegistry...)} method.
   */
  public static <T> CodecRegistry registryFor(final Class<T> type, final TypeAdapter<T> adapter) {
    return new CodecRegistry() {
      @SuppressWarnings("unchecked")
      @Override
      public <X> Codec<X> get(Class<X> clazz) {
        // TODO is this a safe assumption with polymorphism (in repositories) ?
        if (type.isAssignableFrom(clazz)) {
          return (Codec<X>) codecFor(type, adapter);
        } else {
          // let other registries decide
          throw new CodecConfigurationException(String.format("Type %s not supported by this registry", type.getName()));
        }
      }
    };
  }

  /**
   * Creates codec for {@code type} given GSON {@code adapter}. Codec is composed of internal implementations
   * from this class.
   *
   * @see #encoderFor(Class, TypeAdapter)
   * @see #decoderFor(Class, TypeAdapter)
   */
  public static <T> Codec<T> codecFor(final Class<T> type, final TypeAdapter<T> adapter) {
    checkNotNull(type, "type");
    checkNotNull(adapter, "adapter");
    return new Codec<T>() {
      final Decoder<T> decoder = decoderFor(type, adapter);
      final Encoder<T> encoder = encoderFor(type, adapter);
      @Override
      public T decode(BsonReader reader, DecoderContext context) {
        return decoder.decode(reader, context);
      }

      @Override
      public void encode(BsonWriter writer, T value, EncoderContext context) {
        encoder.encode(writer, value, context);
      }

      @Override
      public Class<T> getEncoderClass() {
        return encoder.getEncoderClass();
      }
    };
  }

  /**
   * Creates a "reader" which is able to deserialize binary data into a immutables instance (using GSON type adapter).
   */
  public static <T> Decoder<T> decoderFor(final Class<T> type, final TypeAdapter<T> adapter) {
    checkNotNull(type, "type");
    checkNotNull(adapter, "adapter");
    return new Decoder<T>() {
      @Override
      public T decode(BsonReader reader, DecoderContext decoderContext) {
        if (!(reader instanceof AbstractBsonReader)) {
          throw new UnsupportedOperationException(String.format("Only readers of type %s supported. Yours is %s",
              AbstractBsonReader.class.getName(), reader.getClass().getName()));
        }

        try {
          return adapter.read(new org.immutables.mongo.repository.internal.BsonReader((AbstractBsonReader) reader));
        } catch (IOException e) {
          throw new RuntimeException(String.format("Couldn't encode %s", type), e);
        }
      }
    };
  }

  /**
   * Creates "writer" which can serialize existing immutable instance into <a href="http://bsonspec.org/">BSON format</a>
   * consumed by mongo server.
   */
  public static <T> Encoder<T> encoderFor(final Class<T> type, final TypeAdapter<T> adapter) {
    checkNotNull(type, "type");
    checkNotNull(adapter, "adapter");
    return new Encoder<T>() {
      @Override
      public void encode(org.bson.BsonWriter writer, T value, EncoderContext encoderContext) {
        try {
          adapter.write(new org.immutables.mongo.repository.internal.BsonWriter(writer), value);
        } catch (IOException e) {
          throw new RuntimeException(String.format("Couldn't write value of class %s: %s", type.getName(), value),e);
        }
      }

      @Override
      public Class<T> getEncoderClass() {
        return type;
      }
    };
  }

}
