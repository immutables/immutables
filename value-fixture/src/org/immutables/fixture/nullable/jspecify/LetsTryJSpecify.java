package org.immutables.fixture.nullable.jspecify;

import java.util.List;
import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Value.Immutable
public interface LetsTryJSpecify {
  @Nullable Integer aa();
  @Nullable List<@Nullable String> lst();
}
