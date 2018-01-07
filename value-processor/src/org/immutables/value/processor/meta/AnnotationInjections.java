package org.immutables.value.processor.meta;

import java.lang.annotation.Annotation;
import org.immutables.mirror.Mirror;
import org.immutables.value.processor.meta.AnnotationInjections.InjectAnnotation.Where;

public final class AnnotationInjections {
  private AnnotationInjections() {}

  @Mirror.Annotation("org.immutables.annotate.InjectAnnotation")
  public @interface InjectAnnotation {
    String code() default "";

    Class<? extends Annotation> type() default Override.class;

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

  private static boolean isDefault(String annotationType) {
    return annotationType.equals("org.immutables.annotate.InjectAnnotation") // original default
        || annotationType.equals(Override.class.getName()); // default from the mirror
  }

  public final class InjectionInfo {
    final String code;
    final String annotationType;
    final boolean ifPresent;
    final Where[] target;

    private InjectionInfo(String code, String annotationType, boolean ifPresent, Where[] target) {
      this.code = code;
      this.annotationType = annotationType;
      this.ifPresent = ifPresent;
      this.target = target;
    }
  }
}
