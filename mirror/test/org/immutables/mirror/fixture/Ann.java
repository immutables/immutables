/*
   Copyright 2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.mirror.fixture;

import com.google.common.base.Optional;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import javax.lang.model.element.AnnotationMirror;
import org.immutables.mirror.Mirror;

@Mirror.Annotation("some.sample.annotation.Ann")
public @interface Ann {
  boolean bool() default true;

  char cr() default ' ';

  double dbl() default Double.NEGATIVE_INFINITY;

  Class<?>[] cls() default {};

  Class<?> sing() default Object.class;

  Retention retention() default @Retention(RetentionPolicy.RUNTIME);

  Nest nest() default @Nest;

  String[] vals() default {};

  String value();

  En en() default En.V2;

  enum En {
    V1, V2
  }

  @Mirror.Annotation("some.sample.annotation.Nest")
  public @interface Nest {}

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
      mirror.getAnnotationMirror();
      mirror.annotationType();

      Optional<NestMirror> optNest = NestMirror.find(Collections.<AnnotationMirror>emptyList());

      mirror.nest().equals(optNest.get());
    }
  }
}
