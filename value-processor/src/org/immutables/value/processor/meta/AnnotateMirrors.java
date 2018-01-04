package org.immutables.value.processor.meta;

import java.lang.annotation.Annotation;
import org.immutables.mirror.Mirror;

public class AnnotateMirrors {
  private AnnotateMirrors() {}

  @Mirror.Annotation("org.immutables.annotate.InjectAnnotation")
  public @interface InjectAnnotation {
    String code() default "";

    Class<? extends Annotation> type() default InjectAnnotation.class;

    boolean ifPresent() default false;

    Where[] target();

    enum Where {
      FIELD,
      ACCESSOR,
      SYNTHETIC_FIELDS,
      CONSTRUCTOR_PARAMETER,
      INITIALIZER,
      ELEMENT_INITIALIZER,
      BUILDER_TYPE,
      IMMUTABLE_TYPE
    }
  }
}
