package org.immutables.common.collect;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class InterningOrdinalDomain<S, E extends OrdinalValue<E>> implements OrdinalDomain<E> {

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
