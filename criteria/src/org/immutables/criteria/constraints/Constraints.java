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
package org.immutables.criteria.constraints;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support class for constraints
 */
public final class Constraints {

  private Constraints() {}

  /**
   * This "host" could accepts {@link ConstraintVisitor}s. Allows evaluation of criterias.
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
      throw new UnsupportedOperationException();
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

}
