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

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Standard implementation of {@link Operator} interface. For now, used only
 * as a container.
 */
class SimpleOperator implements Operator {

  private final String name;
  private final Class<?> type;

  SimpleOperator(String name, Class<?> returnType) {
    this.name = Objects.requireNonNull(name, "name");
    this.type = Objects.requireNonNull(returnType, "returnType");
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Class<?> returnType() {
    return type;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    SimpleOperator that = (SimpleOperator) other;
    return Objects.equals(name, that.name) &&
            Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("returnType", returnType().getSimpleName())
            .toString();
  }
}
