package org.immutables.common.collect;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

class Domain implements OrdinalDomain<Ord> {
  private final LoadingCache<Integer, Ord> values =
      CacheBuilder.newBuilder()
          .build(new CacheLoader<Integer, Ord>() {
            @Override
            public Ord load(Integer key) {
              return new Ord(Domain.this, key);
            }
          });

  @Override
  public Ord get(int ordinal) {
    return values.getUnchecked(ordinal);
  }

  @Override
  public int length() {
    return values.asMap().size();
  }
}
