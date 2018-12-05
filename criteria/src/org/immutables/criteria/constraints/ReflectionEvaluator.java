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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import org.immutables.criteria.DocumentCriteria;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Evaluator (predicate) based on reflection. Uses criteria visitor API to construct the predicate.
 * <p>Probably most useful in testing scenarios</p>
 */
public class ReflectionEvaluator<C extends DocumentCriteria<C, T>, T> implements Predicate<T> {

  private final DocumentCriteria<C, T> criteria;

  private ReflectionEvaluator(DocumentCriteria<C, T> criteria) {
    this.criteria = Preconditions.checkNotNull(criteria, "criteria");
  }

  /**
   * Factory method to create refection-based evaluator.
   */
  public static <C extends DocumentCriteria<C, T>, T> ReflectionEvaluator<C, T> of(DocumentCriteria<C, T> criteria) {
    return new ReflectionEvaluator<>(criteria);
  }

  @Override
  public boolean apply(T input) {
    Preconditions.checkNotNull(input, "input");
    final Constraints.ConstraintHost host = (Constraints.ConstraintHost) criteria;
    final LocalVisitor<T> visitor = new LocalVisitor<>(new FieldExtractor<T>(input));
    host.accept(visitor);
    return visitor.result();
  }

  interface ValueExtractor<T> {
    @Nullable
    Object extract(String name);
  }

  private static class FieldExtractor<T> implements ValueExtractor<T> {
    private final T object;
    private final Class<T> klass;

    private FieldExtractor(T object) {
      this.object = object;
      this.klass = (Class<T>) object.getClass();
    }

    @Nullable
    @Override
    public Object extract(String name) {
      try {
        // TODO caching
        final Field field = klass.getDeclaredField(name);
        if (!field.isAccessible()) {
          field.setAccessible(true);
        }
        return field.get(object);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class LocalVisitor<T> implements Constraints.ConstraintVisitor<LocalVisitor<T>> {

    private final ValueExtractor<T> extractor;
    private boolean previous;
    private boolean current;

    private LocalVisitor(ValueExtractor<T> extractor) {
      this.extractor = Preconditions.checkNotNull(extractor, "extractor");
      this.previous = false;
      this.current = true;
    }

    private boolean skipEvaluation() {
      return previous || !current;
    }

    private boolean result() {
      return previous || current;
    }

    @Override
    public LocalVisitor<T> in(String name, boolean negate, Iterable<?> values) {
      if (skipEvaluation()) {
        return this;
      }

      final Object extracted = extractor.extract(name);

      final boolean result = Iterables.any(values, new Predicate<Object>() {
        @Override
        public boolean apply(@Nullable Object input) {
          return Objects.equals(extracted, input);
        }
      });

      current = negate != result;
      return this;
    }

    @Override
    public LocalVisitor<T> equal(String name, boolean negate, @Nullable Object value) {
      if (skipEvaluation()) {
        return this;
      }
      final Object extracted = extractor.extract(name);
      final boolean result = Objects.equals(value, extracted);
      current = negate != result;
      return this;
    }

    @Override
    public LocalVisitor<T> range(String name, boolean negate, Range range) {
      if (skipEvaluation()) {
        return this;
      }

      final Object extracted = extractor.extract(name);
      final boolean result;
      if (extracted == null) {
        result = false;
      } else if (extracted instanceof Comparable) {
        result = range.contains((Comparable<?>) extracted);
      } else {
        throw new IllegalArgumentException(String.format("Not a comparable %s: %s", extracted.getClass(), extracted));
      }

      current = negate != result;
      return this;
    }

    @Override
    public LocalVisitor<T> size(String name, boolean negate, int size) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LocalVisitor<T> present(String name, boolean negate) {
      if (skipEvaluation()) {
        return this;
      }

      final Object extracted = extractor.extract(name);
      final boolean result;
      if (extracted instanceof Optional) {
        result = ((Optional) extracted).isPresent();
      } else {
        // probably java8 Optional
        result = extracted != null;
      }
      current = negate != result;
      return this;
    }

    @Override
    public LocalVisitor<T> match(String name, boolean negate, Pattern pattern) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LocalVisitor<T> nested(String name, Constraints.ConstraintHost constraints) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LocalVisitor<T> disjunction() {
      previous = current;
      current = true;
      return this;
    }
  }
}
