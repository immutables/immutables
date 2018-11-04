/*
   Copyright 2013-2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.mongo.repository.internal;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class Constraints {
  private Constraints() {}

  private static final CharMatcher NON_LITERAL_REGEX_CHARACTERS = CharMatcher.anyOf("\\^$[]().*+").precomputed();

  /**
   * This "host" could accepts {@link ConstraintVisitor}s.
   */
  public interface ConstraintHost {
    <V extends ConstraintVisitor<V>> V accept(V visitor);
  }

  public interface ConstraintVisitor<V extends ConstraintVisitor<V>> {
    V in(String name, boolean negate, Iterable<?> values);

    V equal(String name, boolean negate, @Nullable Object value);

    V range(String name, boolean negate, Range<?> range);

    V size(String name, boolean negate, int size);

    V present(String name, boolean negate);

    V match(String name, boolean negate, Pattern pattern);

    V nested(String name, ConstraintHost constraints);

    V disjunction();
  }

  private static final class PatternConstraint extends ConsConstraint {
    PatternConstraint(Constraint tail, String name, boolean negate, Pattern pattern) {
      super(tail, name, negate, checkNotNull(pattern));
    }

    @Override
    <V extends ConstraintVisitor<V>> V dispatch(V visitor) {
      return visitor.match(name, negate, (Pattern) value);
    }
  }

  private static final class InConstraint extends ConsConstraint {
    InConstraint(Constraint tail, String name, boolean negate, Iterable<?> value) {
      super(tail, name, negate, ImmutableSet.copyOf(value));
    }

    @Override
    <V extends ConstraintVisitor<V>> V dispatch(V visitor) {
      return visitor.in(name, negate, (Iterable<?>) value);
    }
  }

  private static final class EqualToConstraint extends ConsConstraint {
    EqualToConstraint(Constraint tail, String name, boolean negate, Object value) {
      super(tail, name, negate, value);
    }

    @Override
    <V extends ConstraintVisitor<V>> V dispatch(V visitor) {
      return visitor.equal(name, negate, value);
    }
  }

  private static final class RangeConstraint extends ConsConstraint {
    RangeConstraint(Constraint tail, String name, boolean negate, Range<?> value) {
      super(tail, name, negate, checkNotNull(value));
    }

    @Override
    <V extends ConstraintVisitor<V>> V dispatch(V visitor) {
      return visitor.range(name, negate, (Range<?>) value);
    }
  }

  private static final class SizeConstraint extends ConsConstraint {
    SizeConstraint(Constraint tail, String name, boolean negate, int value) {
      super(tail, name, negate, value);
    }

    @Override
    <V extends ConstraintVisitor<V>> V dispatch(V visitor) {
      return visitor.size(name, negate, (Integer) value);
    }
  }

  private static final class PresenseConstraint extends ConsConstraint {
    PresenseConstraint(Constraint tail, String name, boolean negate) {
      super(tail, name, negate, null);
    }

    @Override
    <V extends ConstraintVisitor<V>> V dispatch(V visitor) {
      return visitor.present(name, negate);
    }
  }

  private static final class NestedConstraint extends ConsConstraint {
    NestedConstraint(Constraint tail, String name, ConstraintHost value) {
      super(tail, name, false, value);
    }

    @Override
    <V extends ConstraintVisitor<V>> V dispatch(V visitor) {
      return visitor.nested(name, (ConstraintHost) value);
    }
  }

  private static final class DisjunctionConstraint extends Constraint {
    private final Constraint tail;

    DisjunctionConstraint(Constraint tail) {
      this.tail = tail;
    }

    @Override
    public <V extends ConstraintVisitor<V>> V accept(V visitor) {
      return tail.accept(visitor).disjunction();
    }
  }

  private static final Constraint NIL = new Constraint() {
    @Override
    public final <V extends ConstraintVisitor<V>> V accept(V visitor) {
      return visitor;
    }

    @Override
    public boolean isNil() {
      return true;
    }
  };

  public static Constraint nilConstraint() {
    return NIL;
  }

  public abstract static class Constraint implements ConstraintVisitor<Constraint>, ConstraintHost {
    public boolean isNil() {
      return false;
    }

    @Override
    public Constraint in(String name, boolean negate, Iterable<?> values) {
      return new InConstraint(this, name, negate, values);
    }

    @Override
    public Constraint equal(String name, boolean negate, @Nullable Object value) {
      return new EqualToConstraint(this, name, negate, value);
    }

    @Override
    public Constraint range(String name, boolean negate, Range<?> range) {
      return new RangeConstraint(this, name, negate, range);
    }

    @Override
    public Constraint size(String name, boolean negate, int size) {
      return new SizeConstraint(this, name, negate, size);
    }

    @Override
    public Constraint present(String name, boolean negate) {
      return new PresenseConstraint(this, name, negate);
    }

    @Override
    public Constraint match(String name, boolean negate, Pattern pattern) {
      return new PatternConstraint(this, name, negate, pattern);
    }

    @Override
    public Constraint disjunction() {
      return new DisjunctionConstraint(this);
    }

    @Override
    public Constraint nested(String name, ConstraintHost constraints) {
      return new NestedConstraint(this, name, constraints);
    }
  }

  private abstract static class ConsConstraint extends Constraint {
    final Constraint tail;
    final String name;
    final boolean negate;
    @Nullable
    final Object value;

    ConsConstraint(
        Constraint tail,
        String name,
        boolean negate,
        @Nullable Object value) {
      this.tail = checkNotNull(tail);
      this.name = checkNotNull(name);
      this.value = value;
      this.negate = negate;
    }

    @Override
    public final <V extends ConstraintVisitor<V>> V accept(V visitor) {
      return dispatch(tail.accept(visitor));
    }

    abstract <V extends ConstraintVisitor<V>> V dispatch(V visitor);
  }

  public static Pattern prefixPatternOf(String prefix) {
    checkArgument(NON_LITERAL_REGEX_CHARACTERS.matchesNoneOf(prefix),
        "Prefix [%s] should be literal, otherwise use constructed regex Pattern",
        prefix);
    return Pattern.compile("^" + prefix);
  }

  /**
   * Allows subclasses to implement (visit) just some methods of the interface.
   */
  @SuppressWarnings("unchecked") // T must be self type
  static abstract class AbstractConstraintVisitor<T extends ConstraintVisitor<T>> implements ConstraintVisitor<T> {

    @Override
    public T in(String name, boolean negate, Iterable<?> values) {
      return (T) this;
    }

    @Override
    public T equal(String name, boolean negate, @Nullable Object value) {
      return (T) this;
    }

    @Override
    public T range(String name, boolean negate, Range<?> range) {
      return (T) this;
    }

    @Override
    public T size(String name, boolean negate, int size) {
      return (T) this;
    }

    @Override
    public T present(String name, boolean negate) {
      return (T) this;
    }

    @Override
    public T match(String name, boolean negate, Pattern pattern) {
      return (T) this;
    }

    @Override
    public T nested(String name, ConstraintHost constraints) {
      return (T) this;
    }

    @Override
    public T disjunction() {
      return (T) this;
    }
  }
}
