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
import javax.annotation.concurrent.Immutable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Access to a property or entity (class) possibly via several paths like {@code foo.bar.qux}.
 *
 * <p>Path always starts with an entity (class) which is root. Each subsequent {@link Member} is appended
 * as child. So {@code foo.bar.qux} starts with root which is class of {@code foo} then
 * member {@code foo}, member {@code bar} and member {@code qux}.
 *
 * <p>This class is immutable
 */
@Immutable
public class Path implements Expression {

  private final Path parent;
  private final AnnotatedElement annotatedElement;
  private final Type returnType;

  private Path(Path parent, AnnotatedElement annotatedElement) {
    this.parent = parent;
    this.annotatedElement = Objects.requireNonNull(annotatedElement, "annotatedElement");
    this.returnType = extractReturnType(annotatedElement);
  }

  private static Type extractReturnType(AnnotatedElement element) {
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

  public AnnotatedElement element() {
    return annotatedElement;
  }

  /**
   * Means current path represents an entity (class) not necessarily a member like field or method.
   */
  public boolean isRoot() {
    return parent == null;
  }

  public Path root() {
    return parent != null ? parent.root() : this;
  }

  /**
   * Return list of path members starting with root (excluded) to current element (included).
   * For root path returns empty list.
   */
  public List<Member> members() {
    ImmutableList.Builder<Member> parents = ImmutableList.builder();
    Path current = this;
    while (!current.isRoot()) {
      parents.add((Member) current.element());
      current = current.parent;
    }
    return parents.build().reverse();
  }

  /**
   * Create new instance of path with {@code member} appended
   */
  public Path append(Member member) {
    Objects.requireNonNull(member, "member");
    Preconditions.checkArgument(member instanceof AnnotatedElement, "Expected %s to implement %s", member.getClass(), AnnotatedElement.class);
    return new Path(this, (AnnotatedElement) member);
  }

  public static Path ofMember(Member member) {
    Objects.requireNonNull(member, "member");
    return ofClass(member.getDeclaringClass()).append(member);
  }

  public static Path ofClass(Class<?> type) {
    return new Path(null, type);
  }

  /**
   * Returns current path in java bean format: {@code foo.bar.qux}. For root path returns
   * empty list ({@code ""}).
   */
  public String toStringPath() {
    return members().stream().map(Member::getName).collect(Collectors.joining("."));
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
