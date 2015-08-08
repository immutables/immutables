package org.immutables.fixture.style;

import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(optionalAcceptNullable = false,
    generateSuppressAllWarnings = false)
public interface OptionalWithoutNullable {
  Optional<String> getJavaOptional();

  OptionalInt getJavaOptionalInt();

  com.google.common.base.Optional<String> getGuavaOptional();
}
