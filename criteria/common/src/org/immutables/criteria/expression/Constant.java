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

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A constant. {@code true}, {@code 1}, {@code "foo"}, {@code null} etc.
 */
public final class Constant implements Expression {

  private final Object value;

  private Constant(Object value) {
    this.value = value;
  }

  /**
   * Value of current constant (can be {@code null})
   */
  public Object value() {
    return value;
  }

  /**
   * Converts current value to list (if it is not already). If value
   * is iterable returns that list (which is most likely ImmutableList already).
   *
   * @return singleton list with current value or immutable list of values depending on type
   * of current value.
   */
  public List<Object> values() {
    if (value instanceof Iterable) {
      // most likely ImmutableList already (if Iterable)
      return ImmutableList.copyOf((Iterable<?>) value);
    }

    if (value == null) {
      throw new NullPointerException("value is null");
    }

    return ImmutableList.of(value);
  }

  public static Constant of(Object value) {
    return new Constant(value);
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }
}
