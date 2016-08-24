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
package org.immutables.trees;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Beta
@Target({})
public @interface Trees {
  @Documented
  @Target(ElementType.TYPE)
  public @interface Include {
    Class<?>[] value() default {};
  }

  @Target(ElementType.TYPE)
  public @interface Ast {}

  @Documented
  @Target(ElementType.TYPE)
  public @interface Transform {}

  @Documented
  @Target(ElementType.TYPE)
  public @interface Visit {}
}
