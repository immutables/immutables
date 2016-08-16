package org.immutables.fixture;

import org.immutables.value.Value;

public class BuildFromCovariants {
  interface Identifiable<T> {
    T id();
  }

  interface UniqueIdentifiable {
    Object uid();
  }

  static abstract class Item implements Identifiable<Integer>, UniqueIdentifiable {
    @Override
    public abstract String uid();
  }

  @Value.Immutable
  abstract static class ItemSimple extends Item {}
}
