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

package org.immutables.criteria.backend;

import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Gives an unique (generated) name to objects so they can be used as identifiers in queries.
 * Same objects (as defined by object equality) will have same name.
 *
 * @param <T>
 */
public class UniqueCachedNaming<T> implements Function<T, String> {

  private static final Suggester<?> PREFIX_SUGGESTER = (first, attempts, size) -> "expr" + size;

  private final BiMap<T, String> names;

  private final Converter<T, String> converter;

  private UniqueCachedNaming(Iterable<T> values) {
    Objects.requireNonNull(values, "values");
    this.names = buildBiMap(values);
    this.converter =  new Converter<T, String>() {
      @Override
      protected String doForward(T toName) {
        Preconditions.checkArgument(names.containsKey(toName), "%s was not cached", toName);
        return names.get(toName);
      }

      @Override
      protected T doBackward(String fromName) {
        Preconditions.checkArgument(names.containsValue(fromName), "name %s not found", fromName);
        return names.inverse().get(fromName);
      }
    };
  }

  private static <T> BiMap<T, String> buildBiMap(Iterable<T> values) {
    @SuppressWarnings("unchecked")
    Suggester<T> suggester = (Suggester<T>) PREFIX_SUGGESTER;
    final BiMap<T, String> map = HashBiMap.create();
    for (T value: values) {
      if (!map.containsKey(value)) {
        String name;
        for (int i = 0; ; i++) {
          name = suggester.suggest(value, i, map.size());
          if (!map.containsValue(name)) {
            map.put(value, name);
            break; // name found, break the loop
          }
        }
      }
    }

    return ImmutableBiMap.copyOf(map);
  }

  public Converter<T, String> asConverter() {
    return converter;
  }

  @Override
  public String apply(T value) {
    return asConverter().convert(value);
  }

  /**
   * Suggest new string name for a value
   */
  private interface Suggester<T> {
    String suggest(T value, int attempts, int size);
  }

  public static <T> UniqueCachedNaming<T> of(Iterable<T> iterable) {
    return new UniqueCachedNaming<>(iterable);
  }

  public static <T> UniqueCachedNaming of(Iterator<T> iterator) {
    return of(ImmutableList.copyOf(iterator));
  }

}
