package org.immutables.criteria.constraints;

import org.immutables.criteria.DocumentCriteria;

import java.util.Objects;
import java.util.function.UnaryOperator;

public class CollectionCriteria<R extends DocumentCriteria<R>, S extends DocumentCriteria<R>, C extends DocumentCriteria<?>> implements DocumentCriteria<R> {

  private final CriteriaContext<R> context;

  public CollectionCriteria(CriteriaContext<R> context) {
    this.context = Objects.requireNonNull(context, "context");
  }

  public S all() {
    throw new UnsupportedOperationException();
  }

  public R all(UnaryOperator<C> consumer) {
    throw new UnsupportedOperationException();
  }

  public S none() {
    throw new UnsupportedOperationException();
  }

  public R none(UnaryOperator<C> consumer) {
    throw new UnsupportedOperationException();
  }

  public S any() {
    throw new UnsupportedOperationException();
  }

  public R any(UnaryOperator<C> consumer) {
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

  public R hasSize(int size) {
    throw new UnsupportedOperationException();
  }

  public static class Self extends CollectionCriteria<Self, Self, Self> {
    public Self(CriteriaContext<Self> context) {
      super(context);
    }
  }

}
