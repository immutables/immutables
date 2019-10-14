/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Member;

public interface MemberExtractor {

  /**
   * Extract value of {@code member} from {@code instance}. {@code member} in this case can be
   * {@link Field} or {@link java.lang.reflect.Method} in a class.
   *
   * @param member member to be extracted (can be {@link Field} or {@link java.lang.reflect.Method})
   * @param instance instance to extract value from
   * @throws IllegalArgumentException if specified member argument is not is not member of the
   *      instance class
   * @throws NullPointerException if {@code instance} is null
   *
   * @return value of {@code member} (field or method) in {@code instance}. Return value can be {@code null}.
   */
  Object extract(Member member, Object instance);

  static MemberExtractor ofReflection() {
    return new Reflections.ReflectionExtractor();
  }

}
