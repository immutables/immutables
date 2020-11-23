/*
 * Copyright 2020 Immutables Authors and Contributors
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

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Objects;

/**
 * To preserve compatibility returns {@code null} on {@linkplain CodecConfigurationException}
 */
class LegacyCodecAdapter implements CodecProvider {

  private final CodecRegistry delegate;

  private LegacyCodecAdapter(CodecRegistry delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
    try {
      return delegate.get(clazz);
    } catch (CodecConfigurationException e) {
      return null;
    }
  }

  static CodecProvider of(CodecRegistry registry) {
    return new LegacyCodecAdapter(registry);
  }
}
