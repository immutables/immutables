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
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Access to a property possibly via several paths like {@code foo.bar.qux}
 */
public final class Path implements Expression {

  private final Path parent;
  private final Member member;

  private Path(Path parent, Member member) {
    this.parent = parent;
    this.member = Objects.requireNonNull(member, "member");
  }

  public Optional<Path> parent() {
    return Optional.ofNullable(parent);
  }

  public Member member() {
    return member;
  }

  /**
   * Paths from root to current element
   */
  public List<Member> paths() {
    Path current = this;
    final ImmutableList.Builder<Member> parents = ImmutableList.builder();
    do {
      parents.add(current.member());
      current = current.parent;
    } while (current != null);

    return parents.build().reverse();
  }

  public Path with(Member member) {
    Objects.requireNonNull(member, "member");
    return new Path(this, member);
  }

  public static Path of(Member member) {
    return new Path(null, member);
  }

  /**
   * Returns current path in java bean format: {@code foo.bar.qux}
   */
  public String toStringPath() {
    return paths().stream().map(Member::getName).collect(Collectors.joining("."));
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
}
