package org.immutables.fixture.annotation;

import nonimmutables.ChildAnnotationA;
import nonimmutables.ChildAnnotationB;
import nonimmutables.ParentAnnotation;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(passAnnotations = {ParentAnnotation.class})
public interface ExampleModelWithParentPassAnnotation {
  @ChildAnnotationA
  @ChildAnnotationB
  String attributeWithBothChildAnnotations();
}
