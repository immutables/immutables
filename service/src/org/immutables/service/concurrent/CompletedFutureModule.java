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
package org.immutables.service.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.immutables.common.concurrent.FluentFuture;
import org.immutables.common.concurrent.FluentFutures;

@Beta
public final class CompletedFutureModule implements Module {
  private final Map<Key<?>, Object> instances;

  private CompletedFutureModule(Map<Key<?>, Object> instances) {
    this.instances = instances;
  }

  @Override
  public void configure(Binder binder) {
    for (Entry<Key<?>, Object> entry : instances.entrySet()) {
      // safe cast, key and value known to have corresponding types
      @SuppressWarnings("unchecked")
      Key<Object> key = (Key<Object>) entry.getKey();
      binder.bind(key).toInstance(entry.getValue());
    }
  }

  public static FluentFuture<Module> from(Injector injector) {
    final Map<Key<?>, Key<?>> futureTargetKeys = futureBridgeKeysFor(injector);
    ListenableFuture<List<Object>> allFuture =
        Futures.allAsList(getFutureInstances(futureTargetKeys.keySet(), injector));

    return FluentFutures.from(allFuture)
        .transform(moduleBindingMapper(futureTargetKeys.values()));
  }

  private static Function<List<Object>, Module> moduleBindingMapper(Iterable<Key<?>> targetKeys) {
    final Key<?>[] keys = Iterables.toArray(targetKeys, Key.class);
    return new Function<List<Object>, Module>() {
      @Override
      public Module apply(List<Object> input) {
        ImmutableMap.Builder<Key<?>, Object> builder = ImmutableMap.builder();
        for (int i = 0; i < keys.length; i++) {
          builder.put(keys[i], input.get(i));
        }
        return new CompletedFutureModule(builder.build());
      }
    };
  }

  private static List<ListenableFuture<?>> getFutureInstances(Set<Key<?>> keys, Injector injector) {
    List<ListenableFuture<?>> futures = Lists.newArrayList();
    for (Key<?> key : keys) {
      futures.add((ListenableFuture<?>) injector.getInstance(key));
    }
    return futures;
  }

  private static Map<Key<?>, Key<?>> futureBridgeKeysFor(Injector injector) {
    Map<Key<?>, Key<?>> futureBridgeKeys = Maps.newLinkedHashMap();

    for (Key<?> key : injector.getBindings().keySet()) {
      TypeLiteral<?> typeLiteral = key.getTypeLiteral();

      if (ListenableFuture.class.isAssignableFrom(typeLiteral.getRawType())) {
        ParameterizedType parametrizedType = ((ParameterizedType) typeLiteral.getType());
        TypeLiteral<?> deferefencedType = TypeLiteral.get(parametrizedType.getActualTypeArguments()[0]);
        futureBridgeKeys.put(key, key.ofType(deferefencedType));
      }
    }
    return futureBridgeKeys;
  }
}
