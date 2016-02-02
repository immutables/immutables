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
package org.immutables.mirror;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @see Annotation
 */
@Beta
@Target({})
public @interface Mirror {
  /**
   * Generate annotation mirror handler, by annotation special structurally matching annotation.
   */
  @Beta
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.ANNOTATION_TYPE)
  public @interface Annotation {
    /**
     * Fully qualified canonical name of annotation being modelled.
     * @return fully qualified name.
     */
    String value();
  }
}
