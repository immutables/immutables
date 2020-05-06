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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * A constant. {@code true}, {@code 1}, {@code "foo"}, {@code null} etc.
 */
@Value.Immutable(lazyhash = true)
@Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
public abstract class Constant implements Expression {

  Constant() {}

  /**
   * Value of current constant (can be {@code null})
   */
  @Nullable
  public abstract Object value();

  /**
   * Converts current value to collection (if it is not already). If value
   * is iterable returns that collection (which is most likely ImmutableList already).
   *
   * @return singleton list with current value or immutable list of values depending on type
   * of current value.
   */
  public Collection<?> values() {
    Object value = value();
    if (value instanceof Iterable) {
      if (value instanceof Collection) {
        // already immutable collection
        return (Collection<?>) value;
      }
      // most likely ImmutableList already (if Iterable)
      return ImmutableList.copyOf((Iterable<?>) value);
    }

    Objects.requireNonNull(value, "value is null");
    return ImmutableSet.of(value);
  }

  static Constant ofType(@Nullable Object value, Type type) {
    Object newValue = value;
    if (value instanceof Iterable) {
      if (value instanceof Set) {
        newValue = ImmutableSet.copyOf((Set<?>) value);
      } else {
        newValue = ImmutableList.copyOf((Iterable<?>) value);
      }
    }
    return ImmutableConstant.builder().value(newValue).returnType(type).build();
  }

  static Constant of(Object value) {
    if (value == null) {
      throw new NullPointerException(String.format("value is null. Use %s.ofType(Object, Class<?>)", Constant.class.getSimpleName()));
    }
    return ofType(value, value.getClass());
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass().getSimpleName())
            .add("value", value())
            .add("type", returnType().getTypeName())
            .toString();

  }
}
