/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.annotation;

import com.google.common.annotations.Beta;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use in conjunction with {@link GenerateModifiable}. This annotation, being applied to accessor
 * methods with return types of {@code boolean}, {@code byte}, {@code short} or {@code int} used to
 * generate value stored as compactly packed bits. Many
 * data-types could be effectively reduced to fixed size enumerations or limited range integer
 * values, this gives ultimate opportunity for data size minimization while maintaining efficient
 * access. Internally, generated {@code long} fields are used to store sequences of bits. Any other
 * attributes of the {@code Modifiable*} subclass (which are not annotated with
 * {@code GeneratePackedBits}) will be unaffected by such packing.
 * <p>
 * You should definitely try and see generated {@code Modifiable*} subclass for the details.
 * @see GenerateModifiable
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Beta
@Deprecated
public @interface GeneratePackedBits {
  /**
   * Used to specify order of packed fields. It's defaults to zero and allows for
   * non-contiguous order values (arguments are sorted ascending by this order value).
   * <p>
   * <em>This attribute was introduced as JDT annotation processor internally tracks alphabetical order
   * of members, this differs from Javac, which uses order of declaration appearance. Thus, in order
   * to support portable constructor argument definitions, developer should supply order explicitly.</em>
   * @return order
   */
  int order() default 0;

  /**
   * Minimal integer value that we need to store. Defaults to {@code 0}
   * @return min value
   */
  int min() default 0;

  /**
   * Maximal integer value that we need to store. Defaults to integer.MAX_VALUE
   * @return max value
   */
  int max() default Integer.MAX_VALUE;
}
