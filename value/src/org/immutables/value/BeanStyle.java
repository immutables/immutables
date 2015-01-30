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
package org.immutables.value;

import com.google.common.annotations.Beta;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean related style-customizations. Serve mostly as example
 */
//@Target({})
@Beta
public @interface BeanStyle {

  /**
   * Annotations that applies speculative Java Bean-style accessor naming convention
   * to the generate immutable and other derived classes.
   * It works by being annotated with {@literal @}{@link Value.Style} annotation which specifies
   * customized naming templates. This annotation could be placed on a class, surrounding
   * {@link Value.Nested} class or even a package (declared in {@code package-info.java}). This
   * annotation more of example of how to define your own styles as meta-annotation rather than a
   * useful annotation.
   */
  @Beta
  @Value.Style(get = {"is*", "get*"})
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Accessors {}
}
