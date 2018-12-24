package org.immutables.fixture.nullable;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

interface FromSupertypeNullable {

  interface SuperA {
    @Nullable
    String a();
  }

  interface SuperB {
    @Nullable
    String b();
  }

  @Immutable
  interface Subtype extends SuperA, SuperB {
    @Override
    String a();

    @Override
    String b();
  }
}
