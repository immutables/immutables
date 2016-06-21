package org.immutables.gson.adapter;

import java.util.Arrays;
import java.util.List;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
class CustomGenerics {
  @Value.Immutable
  interface Botts<T> {
    Params<String, T> stringAndT();

    Params<Double, List<Integer>> doubleAndIntlist();
  }

  static class Params<T, V> {
    final T t;
    final V v;

    Params(T t, V v) {
      this.t = t;
      this.v = v;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Params<?, ?>
          && ((Params<?, ?>) obj).t.equals(t)
          && ((Params<?, ?>) obj).v.equals(v);
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  static Botts<Integer> createBotts() {
    return ImmutableBotts.<Integer>builder()
        .stringAndT(new Params<>("AAA", 1))
        .doubleAndIntlist(new Params<>(2.2, Arrays.asList(1, 2, 3)))
        .build();
  }
}
