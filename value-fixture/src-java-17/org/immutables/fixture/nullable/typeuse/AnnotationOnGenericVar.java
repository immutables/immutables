package org.immutables.fixture.nullable.typeuse;

import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Value.Immutable
public interface AnnotationOnGenericVar<T> {
  // Need to make sure that we propagate fallback nullable annotation to the builder parameter
  @Nullable T generic();
  // this one is to compare output, make sure we're not generating extra annotation
  @Nullable String string();
}
