package org.immutables.value.processor.encode;

import java.util.Objects;

public abstract class Eq<Q extends Eq<Q>> {
  private final int hash;

  protected Eq(Object... hash) {
    this.hash = Objects.hash(hash);
  }

	protected abstract boolean eq(Q other);

  @SuppressWarnings("unchecked")
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() == obj.getClass()) {
      return eq((Q) obj);
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return hash;
  }
}
