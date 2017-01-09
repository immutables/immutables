package org.immutables.fixture;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;
import org.immutables.value.Value;

// Compilation test for optinal which are not considered optional for generation
// for #502
@Value.Immutable
public abstract class OptionalNonOptional {
  @Value.Default
  public Optional<Path> getStubBinaryPath() {
    return Optional.empty();
  }

  public abstract com.google.common.base.Optional<? extends Path> stubBinaryPath2();

  public abstract @Nullable Optional<Path> stubBinaryPath3();
}
