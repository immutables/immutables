package org.immutables.fixture;

import com.google.common.base.Optional;
import io.atlassian.fugue.Option;
import org.immutables.value.Value;

@Value.Immutable
public interface OptionalOfOptionalRegular<T> {
  Option<Optional<T>> optionalOfOptional();

  default void use() {
    ImmutableOptionalOfOptionalRegular.<Void>builder()
        .optionalOfOptional(Option.some(Optional.<Void>absent()))
        .build();
  }
}
