package org.immutables.value.processor.encode;

import org.immutables.mirror.Mirror;

interface Mirrors {
  @Mirror.Annotation("org.immutables.encode.Encoding")
  @interface Encoding {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Init")
  @interface Init {
    boolean builder() default true;

    boolean wither() default true;
  }

  @Mirror.Annotation("org.immutables.encode.Encoding.Naming")
  @interface Naming {
    String value();
  }

  @Mirror.Annotation("org.immutables.encode.Encoding.Derive")
  @interface Derive {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Expose")
  @interface Expose {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Impl")
  @interface Impl {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Stringify")
  @interface Stringify {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Copy")
  @interface Copy {}
}
