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

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.List;

public class SimpleRegistry implements TypedRegistry {

  private final CodecRegistry registry;
  private final List<TypedProvider> providers;

  private SimpleRegistry(CodecRegistry registry) {
    this.registry = registry;
    this.providers = ImmutableList.of(new OptionalProvider(), new FallbackProvider(registry));
  }

  @Override
  public <T> Codec<T> get(TypeToken<T> type) {
    for (TypedProvider provider: providers) {
      Codec<T> codec = provider.get(type, this);
      if (codec != null) {
        return codec;
      }
    }
    return null;
  }

  public static TypedRegistry of(CodecRegistry registry) {
    return new SimpleRegistry(registry);
  }

}
