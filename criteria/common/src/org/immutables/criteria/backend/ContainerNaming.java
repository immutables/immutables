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

package org.immutables.criteria.backend;

import com.google.common.base.CaseFormat;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.NamingStrategy;

import java.util.Objects;

/**
 * Resolves container name given a class. Container in this context means table, collection, index etc.
 */
public interface ContainerNaming extends NamingStrategy<Class<?>> {

  /**
   * Resolve container name for a particular type. Container in this context means
   * table, collection, index etc.
   *
   * @throws UnsupportedOperationException if name can't be derived
   * @return name of the container (never null)
   */
  @Override
  String name(Class<?> type);

  /**
   * Resolve container name from {@link Criteria.Repository#name()} annotation.
   */
  ContainerNaming FROM_ANNOTATION = entityClass -> {
    Objects.requireNonNull(entityClass, "entityClass");
    final String name = entityClass.getAnnotation(Criteria.Repository.class).name();
    if (name.isEmpty()) {
      throw new UnsupportedOperationException(String.format("%s.name annotation is not defined on %s",
              Criteria.Repository.class.getSimpleName(), entityClass.getName()));
    }
    return name;
  };

  /**
   * Converts class simple name ({@link Class#getSimpleName()}) to camel format: {@code MyClass} into {@code myClass}
   */
  ContainerNaming FROM_CLASSNAME = entityClass -> {
    Objects.requireNonNull(entityClass, "entityClass");
    return CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL).convert(entityClass.getSimpleName());
  };

  /**
   * Tries to derive name from annotation, if not possible derives it from name
   */
  ContainerNaming DEFAULT = context -> {
    try {
      return FROM_ANNOTATION.name(context);
    } catch (UnsupportedOperationException ignore) {
      return FROM_CLASSNAME.name(context);
    }
  };
}
