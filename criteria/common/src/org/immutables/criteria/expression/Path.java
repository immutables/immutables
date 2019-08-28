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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Access to a property or entity (class) possibly via several paths like {@code foo.bar.qux}
 */
public class Path implements Expression {

  private final Path parent;
  private final AnnotatedElement annotatedElement;
  private final Type returnType;

  Path(Path parent, AnnotatedElement annotatedElement) {
    this.parent = parent;
    this.annotatedElement = Objects.requireNonNull(annotatedElement, "annotatedElement");
    this.returnType = extractReturnType(annotatedElement);
  }

  private static Type extractReturnType(AnnotatedElement element) {
    Objects.requireNonNull(element, "element");
    if (element instanceof Field)  {
      return ((Field) element).getGenericType();
    } else if (element instanceof Method) {
      return ((Method) element).getGenericReturnType();
    } else if (element instanceof Class) {
      return (Class<?>) element;
    }

    throw new IllegalArgumentException(String.format("%s should be %s or %s but was %s", element.getClass().getName(), Field.class.getSimpleName(), Method.class.getSimpleName(), element));
  }

  public Optional<Path> parent() {
    return Optional.ofNullable(parent);
  }

  public AnnotatedElement annotatedElement() {
    return annotatedElement;
  }

  /**
   * Paths from root to current element
   */
  public List<AnnotatedElement> paths() {
    Path current = this;
    final ImmutableList.Builder<AnnotatedElement> parents = ImmutableList.builder();
    do {
      parents.add(current.annotatedElement());
      current = current.parent;
    } while (current != null);

    return parents.build().reverse();
  }

  public Path with(Member member) {
    Objects.requireNonNull(annotatedElement, "annotatedElement");
    return new Path(this, (AnnotatedElement) member);
  }

  public static Path of(Member member) {
    Objects.requireNonNull(member, "member");
    Preconditions.checkArgument(member instanceof AnnotatedElement, "%s instanceof %s",
            member.getClass().getName(),
            AnnotatedElement.class.getSimpleName());
    return of((AnnotatedElement) member);
  }

  public static Path of(AnnotatedElement annotatedElement) {
    if (annotatedElement instanceof Class) {
      return of((Class<?>) annotatedElement);
    }
    return new Path(null, annotatedElement);
  }

  public static EntityPath of(Class<?> entity) {
    return new EntityPath(entity);
  }

  /**
   * Returns current path in java bean format: {@code foo.bar.qux}
   */
  public String toStringPath() {
    final Function<AnnotatedElement, String> fn = elem -> {
      if (elem instanceof Member) {
        return ((Member) elem).getName();
      } else if (elem instanceof Class) {
        return ((Class) elem).getSimpleName();
      }

      throw new IllegalArgumentException("Unknown element " + elem);
    };

    return paths().stream().map(fn).collect(Collectors.joining("."));
  }

  @Override
  public String toString() {
    return toStringPath();
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }

  @Override
  public Type returnType() {
    return returnType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, annotatedElement);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Path path = (Path) o;
    return Objects.equals(parent, path.parent) &&
            Objects.equals(annotatedElement, path.annotatedElement);
  }
}
