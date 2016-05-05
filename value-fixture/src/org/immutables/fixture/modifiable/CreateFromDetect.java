package org.immutables.fixture.modifiable;

import org.immutables.value.Value;

@Value.Style(deepImmutablesDetection = true)
public interface CreateFromDetect {

  @Value.Modifiable
  interface Aaa {
    Bbb bbb();
  }

  @Value.Immutable
  @Value.Modifiable
  interface Bbb {
    int zzz();
  }

  default void use() {
    ModifiableAaa.create()
        .setBbb(ModifiableBbb.create());
  }
}
