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
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.annotations.Beta;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility class to convert to / from {@link CodecRegistry} and {@link ObjectMapper}.
 *
 * <p>Please note that Jackson support is currently experimental.
 */
@Beta
public final class JacksonCodecs {

  /**
   * Selects native BSON codec for ser/deser
   */
  private static final Predicate<JavaType> IS_BSON_TYPE = type -> {
    Class<?> raw = type.getRawClass();
    return BsonValue.class.isAssignableFrom(raw) || Document.class.isAssignableFrom(raw) || Bson.class.isAssignableFrom(raw);
  };


  /**
   * Create {@link CodecRegistry} adapter on the top of existing mapper instance
   */
  public static CodecRegistry registryFromMapper(final ObjectMapper mapper) {
    Objects.requireNonNull(mapper, "mapper");
    return CodecRegistries.fromRegistries(new NativeCodecRegistry(mapper), JacksonCodecRegistry.of(mapper));
  }

  private static <T> Codec<T> findCodecOrNull(CodecRegistry registry, Class<T> type) {
    try {
      return registry.get(type);
    } catch (CodecConfigurationException e) {
      return null;
    }
  }

  private static <T> JsonSerializer<T> serializer(final Codec<T> codec) {
    Objects.requireNonNull(codec, "codec");
    return new CodecSerializer<>(codec);
  }

  private static <T> JsonDeserializer<T> deserializer(final Codec<T> codec) {
    Objects.requireNonNull(codec, "codec");
    return new JsonDeserializer<T>() {
      @Override
      public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        @SuppressWarnings("unchecked")
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

      @Override
      public JsonSerializer<?> findMapSerializer(SerializationConfig config, MapType type, BeanDescription beanDesc, JsonSerializer<Object> keySerializer, TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer) {
        if (IS_BSON_TYPE.test(type)) {
          Codec<?> codec = findCodecOrNull(registry, type.getRawClass());
          return codec == null ? null : serializer(codec);
        }
        return null;
      }
    };
  }

  static Deserializers deserializers(final CodecRegistry registry) {
    Objects.requireNonNull(registry, "registry");
    return new Deserializers.Base() {
      @Override
      public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
        Codec<?> codec = findCodecOrNull(registry, type.getRawClass());
        return codec == null ? null : deserializer(codec);
      }

      @Override
      public JsonDeserializer<?> findMapDeserializer(MapType type, DeserializationConfig config, BeanDescription beanDesc, KeyDeserializer keyDeserializer, TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer) throws JsonMappingException {
        if (IS_BSON_TYPE.test(type)) {
          Codec<?> codec = findCodecOrNull(registry, type.getRawClass());
          return codec == null ? null : deserializer(codec);
        }
        return null;
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
  private static Module module(final CodecRegistry registry) {
    Objects.requireNonNull(registry, "registry");
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

  static class CodecSerializer<T> extends StdSerializer<T> implements Wrapper<Codec<T>> {

    private final Codec<T> codec;

    private CodecSerializer(Codec<T> codec) {
      super(codec.getEncoderClass());
      this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) {
      BsonWriter writer = ((BsonGenerator) gen).unwrap();
      codec.encode(writer, value, EncoderContext.builder().build());
    }

    /**
     * Expose original codec for BSON read/write pass-through
     */
    @Override
    public Codec<T> unwrap() {
      return codec;
    }
  }

  private JacksonCodecs() {}
}
