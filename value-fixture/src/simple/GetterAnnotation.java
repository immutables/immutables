/*
    Copyright 2014 Ievgen Lukash

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
package simple;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Placed here to not fall into org.immutables package
// (our annotation are skipped during copying)
@Retention(RetentionPolicy.RUNTIME)
public @interface GetterAnnotation {
  RetentionPolicy policy();

  Class<?> type();

  String string();

  InnerAnnotation[] value() default {};

  @Retention(RetentionPolicy.RUNTIME)
  public @interface InnerAnnotation {}
}
