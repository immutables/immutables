/*
    Copyright 2013 Immutables.org authors

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

import com.google.common.collect.Interners;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instruct build processor to generate immutable implementation of abstract value object.
 * <p/>
 * <em>Be warned that such immutable object may contain attributes that are not immutable. It is allowed by design, but
 * not every such object will be effectively immutable</em>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateImmutable {

  /**
   * If {@code useSingleton=true}, generates internal singleton object constructed without any
   * specified parameters (all default).
   */
  boolean singleton() default false;

  /**
   * If {@code interned=true} then instances will be strong interned on construction.
   * @see Interners#newStrongInterner()
   */
  boolean interned() default false;

  /**
   * If {@code prehashed=true} then {@code hashCode} will be precomputed on construction.
   * This could speed up collection lookups for objects with lots of attributes and nested objects
   * or, in general, when {@code hashCode} computation is expensive and will be used a lot.
   */
  boolean prehashed() default false;

  /**
   * If {@code useBuilder=false}, disables generation of {@code builder()}.
   */
  boolean builder() default true;
}
