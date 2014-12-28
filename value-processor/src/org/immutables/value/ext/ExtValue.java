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
package org.immutables.value.ext;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Nested;

public @interface ExtValue {
  /**
   * Generate visitor for a set of nested classes.Should only be used on {@link Nested} umbrella
   * classes. Then generated *Visitor class used to switch on type trees nested inside
   */
  @Beta
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.TYPE)
  public @interface Visitor {}

  /**
   * Generate transformer for a set of nested classes. Should only be used on {@link Nested}
   * umbrella classes. Then generated *Transformer class used to exploit and refine transformation
   * of immutable graph.
   */
  @Beta
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.TYPE)
  public @interface Transformer {}

}
