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

package org.immutables.criteria.mongo.codecs;

import com.google.common.reflect.TypeToken;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

/**
 * Tuple codecs for optionals
 */
class OptionalProvider implements TypedProvider {

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public <T> Codec<T> get(TypeToken<T> token, TypedRegistry registry) {
    if (token.getRawType().equals(Optional.class) && (token.getType() instanceof ParameterizedType)) {
      TypeToken<?> newType = TypeToken.of(((ParameterizedType) token.getType()).getActualTypeArguments()[0]);
      Codec<?> delegate = registry.get(newType);
      return new OptionalCodec(token.getRawType(), delegate);
    } else if (token.getRawType().equals(com.google.common.base.Optional.class) && (token.getType() instanceof ParameterizedType)) {
      TypeToken<?> newType = TypeToken.of(((ParameterizedType) token.getType()).getActualTypeArguments()[0]);
      Codec<?> delegate = registry.get(newType);
      return new GuavaCodec(token.getRawType(), delegate);
    }
    return null;
  }

  private static abstract class AbstractOptional<T, O> implements Codec<O> {
    private final Class<O> encoderClass;
    private final Codec<T> delegate;

    private AbstractOptional(Class<O> encoderClass, Codec<T> delegate) {
      this.encoderClass = encoderClass;
      this.delegate = delegate;
    }

    @Override
    public final O decode(BsonReader reader, DecoderContext decoderContext) {
      if (reader.getCurrentBsonType() == BsonType.NULL) {
        reader.readNull();
        return empty();
      }
      return nullable(delegate.decode(reader, decoderContext));
    }

    @Override
    public final void encode(BsonWriter writer, O value, EncoderContext encoderContext) {
      throw new UnsupportedOperationException("Not yet supported");
    }

    @Override
    public Class<O> getEncoderClass() {
      return encoderClass;
    }

    abstract O empty();

    abstract O nullable(T value);
  }

  private static class OptionalCodec<T> extends AbstractOptional<T, Optional<T>> {

    private OptionalCodec(Class<Optional<T>> encoderClass, Codec<T> delegate) {
      super(encoderClass, delegate);
    }

    @Override
    Optional<T> empty() {
      return Optional.empty();
    }

    @Override
    Optional<T> nullable(T value) {
      return Optional.ofNullable(value);
    }
  }

  private static class GuavaCodec<T> extends AbstractOptional<T, com.google.common.base.Optional<T>> {

    private GuavaCodec(Class<com.google.common.base.Optional<T>> encoderClass, Codec<T> delegate) {
      super(encoderClass, delegate);
    }

    @Override
    com.google.common.base.Optional<T> empty() {
      return com.google.common.base.Optional.absent();
    }

    @Override
    com.google.common.base.Optional<T> nullable(T value) {
      return com.google.common.base.Optional.fromNullable(value);
    }
  }
}
