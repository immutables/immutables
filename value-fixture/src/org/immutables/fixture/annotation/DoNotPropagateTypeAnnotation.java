package org.immutables.fixture.annotation;

import org.immutables.value.Value;

@Value.Immutable
public interface DoNotPropagateTypeAnnotation {
  @MethodAndTypeUse
  int a();

  @MethodAndTypeUse
  Object b();

  @TypeUseOnly
  String c();
}
