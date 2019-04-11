package org.immutables.criteria.constraints;

import org.immutables.criteria.DocumentCriteria;

import java.util.Objects;
import java.util.function.Consumer;

public class CollectionCriteria<R extends DocumentCriteria<R>, V, S extends ValueCriteria<R, V>> {

  private final CriteriaContext<R> context;

  public CollectionCriteria(CriteriaContext<R> context) {
    this.context = Objects.requireNonNull(context, "context");
  }

  public S all() {
    throw new UnsupportedOperationException();
  }

  public S none() {
    throw new UnsupportedOperationException();
  }

  public S any() {
    throw new UnsupportedOperationException();
  }

  public R any(Consumer<S> consumer) {
    throw new UnsupportedOperationException();
  }

  public S at(int index) {
    throw new UnsupportedOperationException();
  }

  public R isEmpty() {
    throw new UnsupportedOperationException();
  }

  public R isNotEmpty() {
    throw new UnsupportedOperationException();
  }

}
