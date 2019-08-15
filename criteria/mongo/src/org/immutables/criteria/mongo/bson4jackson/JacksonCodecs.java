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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import org.bson.AbstractBsonReader;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;
import org.immutables.criteria.mongo.Wrapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Utility class to convert to / from {@link CodecRegistry} and {@link ObjectMapper}.
 *
 * <p>Please note that Jackson support is currently experimental.
 */
@Beta
public final class JacksonCodecs {

  private JacksonCodecs() {}

  /**
   * Create {@link CodecRegistry} adapter on the top of existing mapper instance
   */
  public static CodecRegistry registryFromMapper(final ObjectMapper mapper) {
    Preconditions.checkNotNull(mapper, "mapper");
    return new CodecRegistry() {
      @Override
      public <T> Codec<T> get(final Class<T> clazz) {
        final JavaType javaType = mapper.getTypeFactory().constructType(clazz);
        if (!mapper.canSerialize(clazz) || !mapper.canDeserialize(javaType)) {
          throw new CodecConfigurationException(String.format("%s (javaType: %s) not supported by Jackson Mapper", clazz, javaType));
        }
        return new JacksonCodec<>(clazz, mapper);
      }
    };
  }

  /**
   * Registers default BSON codecs {@code BsonValueProvider / ValueCodecProvider / Jsr310} as jackson module
   * so BSON types can be serialized / deserialized
   * @return same mapper instance registered bson codecs(s)
   */
  public static ObjectMapper register(ObjectMapper mapper) {
    Objects.requireNonNull(mapper, "mapper");
    final CodecRegistry registry = CodecRegistries.fromRegistries(
            CodecRegistries.fromProviders(new BsonValueCodecProvider()),
            CodecRegistries.fromProviders(new ValueCodecProvider()),
            CodecRegistries.fromProviders(new Jsr310CodecProvider())
    );

    return mapper.registerModule(module(registry));
  }

  static <T> JsonSerializer<T> serializer(final Codec<T> codec) {
    Objects.requireNonNull(codec, "codec");
    return new CodecSerializer<>(codec);
  }

  static <T> JsonDeserializer<T> deserializer(final Codec<T> codec) {
    Objects.requireNonNull(codec, "codec");
    return new JsonDeserializer<T>() {
      @Override
      public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final BsonReader reader = ((Wrapper<BsonReader>) parser).unwrap();
        return codec.decode(reader, DecoderContext.builder().build());
      }
    };
  }

  static Serializers serializers(final CodecRegistry registry) {
    Objects.requireNonNull(registry, "registry");
    return new Serializers.Base() {
      @Override
      public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
        try {
          Codec<?> codec = registry.get(type.getRawClass());
          return serializer(codec);
        } catch (CodecConfigurationException e) {
          return null;
        }
      }
    };
  }

  static Deserializers deserializers(final CodecRegistry registry) {
    Objects.requireNonNull(registry, "registry");
    return new Deserializers.Base() {
      @Override
      public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
        try {
          final Codec<?> codec = registry.get(type.getRawClass());
          return deserializer(codec);
        } catch (CodecConfigurationException ignore) {
          return null;
        }
      }
    };
  }

  /**
   * Create module from existing provider
   */
  public static Module module(CodecProvider provider) {
    return module(CodecRegistries.fromProviders(provider));
  }

  /**
   * Create module from existing registry
   */
  public static Module module(final CodecRegistry registry) {
    Preconditions.checkNotNull(registry, "registry");
    return new Module() {
      @Override
      public String getModuleName() {
        return JacksonCodecs.class.getSimpleName();
      }

      @Override
      public Version version() {
        return Version.unknownVersion();
      }

      @Override
      public void setupModule(SetupContext context) {
        context.addSerializers(serializers(registry));
        context.addDeserializers(deserializers(registry));
      }

      @Override
      public Object getTypeId() {
        // return null so multiple modules can be registered
        // with same ObjectMapper instance
        return null;
      }
    };
  }

  private static class CodecSerializer<T> extends StdSerializer<T> {

    private final Codec<T> codec;

    private CodecSerializer(Codec<T> codec) {
      super(codec.getEncoderClass());
      this.codec = codec;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
      BsonWriter writer = ((BsonGenerator) gen).unwrap();
      codec.encode(writer, value, EncoderContext.builder().build());
    }
  }

  private static class JacksonCodec<T> implements Codec<T> {

    private final Class<T> clazz;
    private final ObjectMapper mapper;

    private JacksonCodec(Class<T> clazz, ObjectMapper mapper) {
      this.clazz = Preconditions.checkNotNull(clazz, "clazz");
      this.mapper = Preconditions.checkNotNull(mapper, "mapper");
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
      final IOContext ioContext = new IOContext(new BufferRecycler(), null, false);
      final BsonParser parser = new BsonParser(ioContext, 0, (AbstractBsonReader) reader);
      try {
        return mapper.readValue(parser, getEncoderClass());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
      final BsonGenerator generator = new BsonGenerator(0, mapper, writer);
      try {
        mapper.writerFor(getEncoderClass()).writeValue(generator, value);
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
