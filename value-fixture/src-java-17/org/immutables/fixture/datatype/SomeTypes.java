package org.immutables.fixture.datatype;

import javax.annotation.Nullable;
import org.immutables.datatype.Data;
import org.immutables.value.Value;

/** Generating data descriptor in combination of records (with builders) and abstract value types. */
@Data
@Value.Enclosing
public sealed interface SomeTypes {

  @Value.Builder
  record RecAbc(int a, String b, boolean c) implements SomeTypes {}

  @Value.Immutable
  non-sealed interface AbsVal extends SomeTypes {
    int a();

    @Nullable String b();

    @Value.Default
    default int n() {
      return 1;
    }
  }
}
