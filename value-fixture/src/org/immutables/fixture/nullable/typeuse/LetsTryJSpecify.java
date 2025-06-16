//-!no-import-rewrite
package org.immutables.fixture.nullable.typeuse;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Value.Immutable
@Value.Modifiable
@Value.Style(optionalAcceptNullable = true, headerComments = true, jdkOnly = true)
public interface LetsTryJSpecify {
  @Nullable Integer aa();
  @Nullable List<@Nullable String> lst();
  @Nullable List<String> stl();
  Optional<String> opt();
}
