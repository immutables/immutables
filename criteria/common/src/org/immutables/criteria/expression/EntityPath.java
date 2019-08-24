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

package org.immutables.criteria.expression;

import java.util.Objects;

/**
 * Used to access concrete entity (class). Different from property
 * access of that class.
 *
 * <p> Example
 * <pre>
 *   {@code Person} - EntityPath
 *   {@code Person.age} - simple Path
 * </pre>
 */
public class EntityPath extends Path {

  private final Class<?> entityClass;

  EntityPath(Class<?> entityClass) {
    super(null, entityClass);
    this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
  }

  @Override
  public Class<?> annotatedElement() {
    return entityClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    EntityPath that = (EntityPath) o;
    return Objects.equals(entityClass, that.entityClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), entityClass);
  }
}
