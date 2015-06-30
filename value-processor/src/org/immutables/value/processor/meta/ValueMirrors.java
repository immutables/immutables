/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import java.lang.annotation.Annotation;
import org.immutables.mirror.Mirror;

public final class ValueMirrors {
  private ValueMirrors() {}

  @Mirror.Annotation("org.immutables.value.Value")
  public @interface ValueUmbrella {}

  @Mirror.Annotation("org.immutables.value.Value.Immutable")
  public @interface Immutable {

    boolean singleton() default false;

    boolean intern() default false;

    boolean copy() default false;

    boolean prehash() default false;

    boolean builder() default true;
  }

  @Mirror.Annotation("org.immutables.value.Value.Include")
  public @interface Include {
    Class<?>[] value();
  }

  @Mirror.Annotation("org.immutables.value.Value.Enclosing")
  public @interface Enclosing {}

  @Mirror.Annotation("org.immutables.value.Value.Derived")
  public @interface Derived {}

  @Mirror.Annotation("org.immutables.value.Value.Default")
  public @interface Default {}

  @Mirror.Annotation("org.immutables.value.Value.Auxiliary")
  public @interface Auxiliary {}

  @Mirror.Annotation("org.immutables.value.Value.Lazy")
  public @interface Lazy {}

  @Mirror.Annotation("org.immutables.value.Value.Parameter")
  public @interface Parameter {
    int order() default 0;
  }

  @Mirror.Annotation("org.immutables.value.Value.Check")
  public @interface Check {}

  @Mirror.Annotation("org.immutables.value.Value.NaturalOrder")
  public @interface NaturalOrder {}

  @Mirror.Annotation("org.immutables.value.Value.ReverseOrder")
  public @interface ReverseOrder {}

  @Mirror.Annotation("org.immutables.value.Value.Modifiable")
  public @interface Modifiable {}

  @Mirror.Annotation("org.immutables.value.Value.Style")
  public @interface Style {
    String[] get() default {};

    String init() default "*";

    String with() default "with*";

    String add() default "add*";

    String addAll() default "addAll*";

    String put() default "put*";

    String putAll() default "putAll*";

    String copyOf() default "copyOf";

    String of() default "of";

    String instance() default "of";

    String builder() default "builder";

    String newBuilder() default "new";

    String from() default "from";

    String build() default "build";

    String typeBuilder() default "Builder";

    String[] typeAbstract() default {};

    String typeImmutable() default "Immutable*";

    String typeImmutableEnclosing() default "Immutable*";

    String typeImmutableNested() default "*";

    Immutable defaults() default @Immutable;

    boolean strictBuilder() default false;

    boolean allParameters() default false;

    boolean defaultAsDefault() default false;

    boolean jdkOnly() default false;

    Class<? extends Annotation>[] passAnnotations() default {};

    ImplementationVisibility visibility() default ImplementationVisibility.SAME;

    public enum ImplementationVisibility {
      PUBLIC,
      SAME,
      SAME_NON_RETURNED,
      PACKAGE,
      PRIVATE
    }
  }
}
