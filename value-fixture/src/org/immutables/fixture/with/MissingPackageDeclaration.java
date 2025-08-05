package org.immutables.fixture.with;

import org.immutables.value.Value;

// this setup should generate no imports in With* interface file
@Value.Style(allowedClasspathAnnotations = Override.class)
@Value.Immutable
public interface MissingPackageDeclaration extends WithMissingPackageDeclaration {
  // only primitive
  int a();
  boolean b();
}
