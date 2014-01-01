/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.common.collect.internal;

import org.immutables.common.collect.OrdinalDomain;
import org.immutables.common.collect.OrdinalValue;
import com.google.common.annotations.Beta;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Beta
public abstract class InterningOrdinalDomain<S, E extends OrdinalValue<E>> extends OrdinalDomain<E> {

  private final List<E> values = new CopyOnWriteArrayList<>();

  private final LoadingCache<S, E> cache =
      CacheBuilder.newBuilder()
          .concurrencyLevel(1)
          .build(new CacheLoader<S, E>() {
            @Override
            public E load(S key) throws Exception {
              E value = extractValue(key, values.size());
              values.add(value);
              return value;
            }
          });

  public final E internOrdinal(S valueSample) {
    return cache.getUnchecked(valueSample);
  }

  protected abstract E extractValue(S valueSample, int ordinal);

  @Override
  public final E get(int ordinal) {
    return values.get(ordinal);
  }

  @Override
  public final int length() {
    return values.size();
  }
}
