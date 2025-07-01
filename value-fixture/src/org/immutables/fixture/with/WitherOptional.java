package org.immutables.fixture.with;

import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public abstract class WitherOptional {
  public abstract AnImm aa();

  public abstract Optional<AnImm> bb();

  public abstract @Nullable Optional<AnImm> cc();
}
