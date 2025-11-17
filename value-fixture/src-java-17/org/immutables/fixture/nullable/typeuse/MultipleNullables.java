package org.immutables.fixture.nullable.typeuse;

import org.immutables.value.Value;
import org.jspecify.annotations.Nullable;

@Value.Immutable
@Value.Style(fallbackNullableAnnotation = Nullable.class)
public abstract class MultipleNullables {
  public abstract @Nullable String ss();
  public abstract @javax.annotation.Nullable Integer ii();
  public abstract @org.eclipse.jdt.annotation.Nullable Double dd();
}
