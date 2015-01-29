package org.immutables.mirror.fixture;

import com.google.common.base.Optional;
import java.util.Collections;
import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import org.immutables.mirror.Mirror;

@Mirror.Annotation("some.sample.annotation.Ann")
public @interface Ann {
  boolean bool() default true;

  char cr() default ' ';

  double dbl() default Double.NEGATIVE_INFINITY;

  Class<?>[] cls() default {};

  Class<?> sing() default Object.class;

  Retention retention() default @Retention(RetentionPolicy.RUNTIME);

  String[] vals() default {};

  String value();

  En en() default En.V2;

  enum En {
    V1, V2
  }

  static class CompiledUse {
    void use() {
      Optional<AnnMirror> optional = AnnMirror.find(Collections.<AnnotationMirror>emptyList());
      AnnMirror mirror = optional.get();
      mirror.clsMirror().clone();
      mirror.singMirror().getKind();
      mirror.clsName().clone();
      mirror.cr();
      mirror.en().name();
      mirror.bool();
      mirror.vals().clone();
      mirror.value();
      mirror.getMirror();
      mirror.annotationType();
    }
  }
}
