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

package org.immutables.criteria;

import org.immutables.criteria.repository.Facet;
import org.immutables.criteria.repository.reactive.ReactiveReadable;
import org.immutables.criteria.repository.reactive.ReactiveWritable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An immutable class annotated with {@code @Criteria} will have Criteria class automatically generated.
 *
 * Generated criteria class is a type-safe query DSL modeled after current type.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Criteria {

  /**
   * Marks attribute as id (primary key) for a particular class.
   */
  @Documented
  @Target({ElementType.METHOD, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Id {}

  /**
   * <p>
   * Classes annotated with this annotation will have repository generated to
   * store and retrieve documents. Repository classes are generated in the same package and
   * and have same visibility as original class (public / package private). They are named
   * after annotated value type: {@code [name_of_annotated_type]Repository}.
   * </p>
   * <p>
   * {@code @Criteria.Repository} should only be used with value types annotated with
   * {@code @Value.Immutable}.
   * </p>
   */
  @Documented
  @Target(ElementType.TYPE)
  @interface Repository {

    /**
     * Allows defining repository properties like readable / writable / watchable etc.
     * @return list of facets the repository should support
     */
    Class<? extends Facet>[] facets() default {ReactiveReadable.class, ReactiveWritable.class};
  }


}
