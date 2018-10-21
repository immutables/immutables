/*
   Copyright 2013-2018 Immutables Authors and Contributors

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
package org.immutables.mongo.bson4gson;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bson.AbstractBsonReader;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.io.IOException;

/**
 * Set of utilities to bridge <a href="http://bsonspec.org/">BSON</a> and
 * <a href="https://github.com/google/gson">Gson</a> standard classes like
 * {@link TypeAdapter} / {@link Codec}(s).
 */
public final class GsonCodecs {

  private GsonCodecs() {
  }

  /**
   * Build a TypeAdapter from {@link Codec} opposite of {@link #codecFromTypeAdapter(Class, TypeAdapter)}.
   *
   * @param codec existing codec
   * @return type adapter which delegates calls to a codec.
   */
  public static <T> TypeAdapter<T> typeAdapterFromCodec(final Codec<T> codec) {
    Preconditions.checkNotNull(codec, "codec");
    return new TypeAdapter<T>() {
      @Override
      public void write(JsonWriter out, T value) throws IOException {
        BsonWriter writer = (BsonWriter) out;
        org.bson.BsonWriter delegate = writer.unwrap();
        codec.encode(delegate, value, EncoderContext.builder().build());
      }

      @Override
      public T read(JsonReader in) throws IOException {
        BsonReader reader = (BsonReader) in;
        org.bson.BsonReader delegate = reader.unwrap();
        return codec.decode(delegate, DecoderContext.builder().build());
      }
    };
  }

  /**
   * Gson Factory which gives preference to existing adapters from {@code gson} instance. However,
   * if type is not supported it will query {@link CodecRegistry} to create one (if possible).
   *
   * <p>This allows supporting Bson types by Gson natively (eg. for {@link org.bson.types.ObjectId}).
   *
   * @param registry existing registry which will be used if type is unknown to {@code gson}.
   * @return factory which delegates to {@code registry} for unknown types.
   */
  public static TypeAdapterFactory delegatingTypeAdapterFactory(final CodecRegistry registry) {
    Preconditions.checkNotNull(registry, "registry");
    return new TypeAdapterFactory() {
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        boolean hasAdapter;
        try {
          TypeAdapter<T> adapter = gson.getDelegateAdapter(this, type);
          hasAdapter = !isReflectiveTypeAdapter(adapter);
        } catch (IllegalArgumentException e) {
          hasAdapter = false;
        }

        if (hasAdapter) {
          return null;
        }

        try {
          @SuppressWarnings("unchecked")
          Codec<T> codec = (Codec<T>) registry.get(type.getRawType());
          return typeAdapterFromCodec(codec);
        } catch (CodecConfigurationException e1) {
          return null;
        }

      }
    };
  }

  /**
   * Build a codec from {@link TypeAdapter}. Opposite of {@link #typeAdapterFromCodec(Codec)}.
   *
   * @param type type handled by this adapter
   * @param adapter existing adapter
   * @param <T> codec value type
   * @throws CodecConfigurationException if adapter is not supported
   * @return new instance of the codec which handles {@code type}.
   */
  public static <T> Codec<T> codecFromTypeAdapter(Class<T> type, TypeAdapter<T> adapter) {
    if (isReflectiveTypeAdapter(adapter)) {
      throw new CodecConfigurationException(String.format("%s can't be build from %s " +
                      "(for type %s)", TypeAdapterCodec.class.getSimpleName(),
              adapter.getClass().getName(), type.getName()));
    }
    return new TypeAdapterCodec<>(type, adapter);
  }

  /**
   * Given existing {@code Gson} instance builds a {@link CodecRegistry}.
   *
   * @param gson preconfigured instance
   * @return wrapper for {@code gson}.
   */
  public static CodecRegistry codecRegistryFromGson(final Gson gson) {
    Preconditions.checkNotNull(gson, "gson");
    return new CodecRegistry() {
      @Override
      public <T> Codec<T> get(Class<T> clazz) {
        return codecFromTypeAdapter(clazz, gson.getAdapter(clazz));
      }
    };
  }

  static <A> boolean isReflectiveTypeAdapter(TypeAdapter<A> adapter) {
    Preconditions.checkNotNull(adapter, "adapter");
    return adapter instanceof com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.Adapter;
  }

  /**
   * Codec which delegates all calls to existing type adapter.
   *
   * @param <T> type handled by this codec
   */
  private static class TypeAdapterCodec<T> implements Codec<T> {
    private final Class<T> clazz;
    private final TypeAdapter<T> adapter;

    private TypeAdapterCodec(Class<T> type, TypeAdapter<T> adapter) {
      this.clazz = Preconditions.checkNotNull(type, "type");
      Preconditions.checkArgument(!isReflectiveTypeAdapter(adapter),
              "Type adapter %s for type '%s' is not supported."
                      + " This may happen when using default RepositorySetup.forUri and"
                      + " META-INF/services/..TypeAdapterFactory files are not compiled or accessible."
                      + " Alternatively this may happen if creating custom RepositorySetup with Gson instance,"
                      + " which does not have type adapters registered.", adapter.getClass().getName(), type);
      this.adapter = adapter;
    }

    @Override
    public T decode(org.bson.BsonReader reader, DecoderContext decoderContext) {
      if (!(reader instanceof AbstractBsonReader)) {
        throw new UnsupportedOperationException(String.format("Only readers of type %s supported. Yours is %s",
                AbstractBsonReader.class.getName(), reader.getClass().getName()));
      }

      try {
        return adapter.read(new BsonReader((AbstractBsonReader) reader));
      } catch (IOException e) {
        throw new RuntimeException(String.format("Couldn't read %s", clazz), e);
      }
    }

    @Override
    public void encode(org.bson.BsonWriter writer, T value, EncoderContext encoderContext) {
      try {
        adapter.write(new BsonWriter(writer), value);
      } catch (IOException e) {
        throw new RuntimeException(String.format("Couldn't write value of class %s: %s", clazz.getName(), value), e);
      }
    }

    @Override
    public Class<T> getEncoderClass() {
      return clazz;
    }

  }
}
