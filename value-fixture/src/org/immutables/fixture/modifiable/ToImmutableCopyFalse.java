package org.immutables.fixture.modifiable;

import org.immutables.value.Value;

// Compilation test to have toImmutable on modifiable companion class when copy = false
public interface ToImmutableCopyFalse {
  @Value.Modifiable
  @Value.Immutable(copy = false)
  public interface A {

    default void use() {
      ModifiableA.create().toImmutable();
    }
  }

  @Value.Modifiable
  @Value.Immutable(copy = false)
  @Value.Style(strictBuilder = true)
  public interface B {

    default void use() {
      ModifiableB.create().toImmutable();
    }
  }

  @Value.Modifiable
  @Value.Immutable(copy = false, builder = false)
  public interface C {
    @Value.Parameter
    int c();

    default void use() {
      ModifiableC.create().toImmutable();
    }
  }
}
