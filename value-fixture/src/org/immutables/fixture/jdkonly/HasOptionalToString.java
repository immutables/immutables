package org.immutables.fixture.jdkonly;

import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import java.util.OptionalInt;

@Value.Immutable
interface HasOptionalToString {
  OptionalInt into();

  String mandatory();

  @Nullable
  String nullable();

  Optional<String> optional();
}
