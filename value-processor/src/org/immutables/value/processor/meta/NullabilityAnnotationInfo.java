package org.immutables.value.processor.meta;

import java.lang.annotation.ElementType;
import javax.lang.model.element.TypeElement;
import org.immutables.value.Value;

@Value.Immutable(intern = true, builder = false)
abstract class NullabilityAnnotationInfo {
  @Value.Parameter
  @Value.Auxiliary
  abstract TypeElement element();

  @Value.Derived
  String qualifiedName() {
    return element().getQualifiedName().toString();
  }

  @Value.Lazy
  String asPrefix() {
    return "@" + qualifiedName() + " ";
  }

  @Value.Lazy
  String asLocalPrefix() {
    boolean applicableToLocal = Annotations.annotationMatchesTarget(
        element(),
        ElementType.LOCAL_VARIABLE);

    return applicableToLocal
        ? asPrefix()
        : "";
  }
}
