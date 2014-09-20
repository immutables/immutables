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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instruct processor to generate immutable implementation of abstract value type.
 * <p>
 * <em>Be warned that such immutable object may contain attributes that are not recursively immutable, thus
 * not every object will be completely immutable. While this may be useful for some workarounds,
 * one should generally avoid creating immutable object with attribute values that could be mutated</em>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateImmutable {

  boolean nonpublic() default false;

  /**
   * If {@code singleton=true}, generates internal singleton object constructed without any
   * specified parameters. Default is {@literal false}.
   */
  boolean singleton() default false;

  /**
   * If {@code intern=true} then instances will be strong interned on construction.
   * Default is {@literal false}.
   * @see com.google.common.collect.Interners#newStrongInterner()
   */
  boolean intern() default false;

  /**
   * If {@code withers=false} then generation of copying methods starting with
   * "withAttributeName" will be disabled. Default is {@literal true}.
   */
  boolean withers() default true;

  /**
   * If {@code prehash=true} then {@code hashCode} will be precomputed during construction.
   * This could speed up collection lookups for objects with lots of attributes and nested objects.
   * In general, use this when {@code hashCode} computation is expensive and will be used a lot.
   */
  boolean prehash() default false;

  /**
   * If {@code builder=false}, disables generation of {@code builder()}. Default is {@literal true}.
   */
  boolean builder() default true;
}
