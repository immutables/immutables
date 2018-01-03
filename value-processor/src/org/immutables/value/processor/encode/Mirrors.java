/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.value.processor.encode;

import org.immutables.mirror.Mirror;

interface Mirrors {
  @Mirror.Annotation("org.immutables.encode.Encoding")
  @interface Encoding {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Naming")
  @interface Naming {
    String value();

    boolean depluralize() default false;

    StandardNaming standard() default StandardNaming.NONE;
  }

  @Mirror.Annotation("org.immutables.encode.Encoding.Of")
  @interface Of {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Expose")
  @interface Expose {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Impl")
  @interface Impl {
    boolean virtual() default false;
  }

  @Mirror.Annotation("org.immutables.encode.Encoding.Copy")
  @interface Copy {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Init")
  @interface Init {}

  @Mirror.Annotation("org.immutables.encode.Encoding.IsInit")
  @interface IsInit {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Builder")
  @interface Builder {}

  @Mirror.Annotation("org.immutables.encode.Encoding.Build")
  @interface Build {}

  @Mirror.Annotation("org.immutables.encode.EncodingMetadata")
  public @interface EncMetadata {
    String name();

    String[] imports();

    String[] typeParams();

    EncElement[] elements();
  }

  @Mirror.Annotation("org.immutables.encode.EncodingMetadata.Element")
  public @interface EncElement {
    String name();

    String type();

    String naming();

    String stdNaming();

    String[] tags();

    String[] typeParams();

    String[] params();

    String[] thrown();

    String[] annotations() default {};

    String[] doc() default {};

    String code();
  }
}
