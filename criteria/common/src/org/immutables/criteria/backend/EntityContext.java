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

import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * Context for a single class (aka entity).
 */
public class EntityContext implements Backend.Context {

  private final Class<?> entityClass;

  private EntityContext(Class<?> entityClass) {
    this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
  }

  public Class<?> entityClass() {
    return entityClass;
  }

  public static EntityContext of(Class<?> entityClass) {
    return new EntityContext(entityClass);
  }

  /**
   * Opposite of {@link #of(Class)} returns entity class for this context
   */
  public static Class<?> extractEntity(Backend.Context context) {
    Objects.requireNonNull(context, "context");
    Preconditions.checkArgument(context instanceof EntityContext, "%s is not %s",
            context.getClass().getName(), EntityContext.class.getName());

    return ((EntityContext) context).entityClass();
  }
}
