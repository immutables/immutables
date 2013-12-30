/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import static com.google.common.base.Preconditions.*;

/**
 * Immutable set that take advantage of elements being an {@link OrdinalValue}s to provide
 * compact storage and efficient {@link Set#contains(Object)} and
 * {@link Set#containsAll(Collection)} operations.
 * @see OrdinalValue
 * @see OrdinalDomain
 * @see BitSet BitSet for similar internal implementation
 * @param <E> element type
 */
@Beta
public abstract class ImmutableOrdinalSet<E extends OrdinalValue<E>>
    extends ForwardingSet<E> {

  ImmutableOrdinalSet() {}

  private static final ImmutableOrdinalSet<? extends OrdinalValue<?>> EMPTY_SET = new EmptyImmutableOrdinalSet<>();

  /**
   * Returns singleton empty immutable ordinal set
   * @param <E> element type
   * @return empty set
   */
  @SuppressWarnings("unchecked")
  public static <E extends OrdinalValue<E>> ImmutableOrdinalSet<E> of() {
    // safe unchecked: will contain no elements
    return (ImmutableOrdinalSet<E>) EMPTY_SET;
  }

  /**
   * Creates immutable ordinal set from 1 or more elements.
   * All elements expected to have same {@link OrdinalValue#ordinalDomain()} as the first element,
   * otherwise exception will be thrown.
   * @param <E> element type
   * @param first first element
   * @param rest the rest of elements
   * @return empty set
   */
  @SafeVarargs
  public static <E extends OrdinalValue<E>> ImmutableOrdinalSet<E> of(
      E first, E... rest) {
    OrdinalDomain<E> domain = first.ordinalDomain();
    if (rest.length == 0) {
      return new SingletonImmutableOrdinalSet<>(first);
    }
    OrdinalValue<?>[] array = new OrdinalValue<?>[1 + rest.length];
    array[0] = first;
    System.arraycopy(rest, 0, array, 1, rest.length);
    return new RegularImmutableOrdinalSet<>(domain, array);
  }

  /**
   * Creates immutable ordinal set from iterable of elements.
   * All elements expected to have same {@link OrdinalValue#ordinalDomain()} as the first element,
   * otherwise exception will be thrown.
   * @param <E> the element type
   * @param elements the elements
   * @return the immutable ordinal set
   */
  @SuppressWarnings("unchecked")
  public static <E extends OrdinalValue<E>> ImmutableOrdinalSet<E> copyOf(Iterable<E> elements) {
    if (elements instanceof ImmutableOrdinalSet) {
      return (ImmutableOrdinalSet<E>) elements;
    }
    OrdinalValue<?>[] array = Iterables.toArray(elements, OrdinalValue.class);
    switch (array.length) {
    case 0:
      return of();
    case 1:
      // Safe unchecked as element is known to be of type E
      return new SingletonImmutableOrdinalSet<>((E) array[0]);
    default:
      return new RegularImmutableOrdinalSet<>(((E) array[0]).ordinalDomain(), array);
    }
  }

  /**
   * Will throw an exception and leave the collection unmodified.
   * @throws UnsupportedOperationException always
   * @deprecated Modification operation are not supported.
   */
  @Deprecated
  @Override
  public final boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  /**
   * Will throw an exception and leave the collection unmodified.
   * @throws UnsupportedOperationException always
   * @deprecated Modification operation are not supported.
   */
  @Deprecated
  @Override
  public final boolean remove(Object object) {
    throw new UnsupportedOperationException();
  }

  /**
   * Will throw an exception and leave the collection unmodified.
   * @throws UnsupportedOperationException always
   * @deprecated Modification operation are not supported.
   */
  @Deprecated
  @Override
  public final boolean addAll(Collection<? extends E> newElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Will throw an exception and leave the collection unmodified.
   * @throws UnsupportedOperationException always
   * @deprecated Modification operation are not supported.
   */
  @Deprecated
  @Override
  public final boolean removeAll(Collection<?> oldElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Will throw an exception and leave the collection unmodified.
   * @throws UnsupportedOperationException always
   * @deprecated Modification operation are not supported.
   */
  @Deprecated
  @Override
  public final boolean retainAll(Collection<?> elementsToKeep) {
    throw new UnsupportedOperationException();
  }

  /**
   * Will throw an exception and leave the collection unmodified.
   * @throws UnsupportedOperationException always
   * @deprecated Modification operation are not supported.
   */
  @Deprecated
  @Override
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  private static class EmptyImmutableOrdinalSet<E extends OrdinalValue<E>>
      extends ImmutableOrdinalSet<E> {
    @Override
    protected Set<E> delegate() {
      return ImmutableSet.of();
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public int size() {
      return 0;
    }
  }

  private static class SingletonImmutableOrdinalSet<E extends OrdinalValue<E>>
      extends ImmutableOrdinalSet<E> {
    private final E element;

    SingletonImmutableOrdinalSet(E element) {
      this.element = checkNotNull(element);
    }

    @Override
    public boolean contains(Object object) {
      return element.equals(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
      if (collection instanceof SingletonImmutableOrdinalSet) {
        return element.equals(((SingletonImmutableOrdinalSet<?>) collection).element);
      }
      return super.containsAll(collection);
    }

    @Override
    protected Set<E> delegate() {
      return ImmutableSet.of(element);
    }

    @Override
    public int size() {
      return 1;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }
  }

  private static class RegularImmutableOrdinalSet<E extends OrdinalValue<E>> extends ImmutableOrdinalSet<E> {
    private static final int WORD_BITS = 64;
    private static final int POWER_OF_TWO_WORD_BITS = 6;

    private final OrdinalDomain<E> domain;
    private final long[] vector;
    private final int size;

    RegularImmutableOrdinalSet(OrdinalDomain<E> domain, OrdinalValue<?>[] elements) {
      int maxOrdinal = 0;
      int count = 0;
      for (OrdinalValue<?> e : elements) {
        checkArgument(e.ordinalDomain().equals(domain), "Element has different domain %s", e);
        maxOrdinal = Math.max(maxOrdinal, e.ordinal());
        count++;
      }
      this.domain = domain;
      this.size = count;
      this.vector = new long[(maxOrdinal >>> POWER_OF_TWO_WORD_BITS) + 1];
      fillVector(elements);
    }

    private void fillVector(OrdinalValue<?>[] elements) {
      for (OrdinalValue<?> e : elements) {
        int ordinal = e.ordinal();
        int wordIndex = ordinal >>> POWER_OF_TWO_WORD_BITS;
        int bitIndex = ordinal - (wordIndex << POWER_OF_TWO_WORD_BITS);
        long word = vector[wordIndex];
        if (((word >>> bitIndex) & 1) != 0) {
          checkArgument(false, "Duplicate element %s", e);
        }
        vector[wordIndex] = word | (1 << bitIndex);
      }
    }

    @Override
    protected Set<E> delegate() {
      ImmutableSet.Builder<E> builder = ImmutableSet.builder();

      for (int i = 0; i < vector.length; i++) {
        long word = vector[i];
        for (int j = 0; j < WORD_BITS; j++) {
          if (((word >> j) & 1) != 0) {
            int ordinal = i * WORD_BITS + j;
            builder.add(domain.get(ordinal));
          }
        }
      }

      return builder.build();
    }

    @Override
    public boolean contains(Object object) {
      if (object instanceof OrdinalValue<?>) {
        OrdinalValue<?> value = (OrdinalValue<?>) object;
        if (value.ordinalDomain().equals(domain)) {
          return containsOrdinal(value.ordinal());
        }
      }
      return false;
    }

    private boolean containsOrdinal(int ordinal) {
      int wordIndex = ordinal >>> POWER_OF_TWO_WORD_BITS;
      int bitIndex = ordinal - (wordIndex << POWER_OF_TWO_WORD_BITS);
      return ((vector[wordIndex] >> bitIndex) & 1) != 0;
    }

    private boolean containsAllOrdinals(RegularImmutableOrdinalSet<?> ordinalSet) {
      long[] otherVector = ordinalSet.vector;
      long[] vector = this.vector;

      if (vector.length < otherVector.length) {
        // If other set contains more words - then it contains higher ordinals that this
        // just don't posses, so containsAll will be false
        return false;
      }

      for (int i = 0; i < otherVector.length; i++) {
        long v = vector[i];
        long ov = otherVector[i];
        if ((v | ov) != v) {
          return false;
        }
      }

      return true;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
      int size = collection.size();
      if (size == 0) {
        return true;
      }
      if (size == 1) {
        return contains(Iterables.get(collection, 0));
      }
      if (collection instanceof RegularImmutableOrdinalSet<?>) {
        RegularImmutableOrdinalSet<?> otherOrdinalSet = (RegularImmutableOrdinalSet<?>) collection;
        return otherOrdinalSet.domain.equals(domain) && containsAllOrdinals(otherOrdinalSet);
      }
      return super.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public int size() {
      return size;
    }
  }

  /**
   * Build instances of {@link ImmutableOrdinalSet}
   * @param <E> element type
   * @return builder
   */
  public static <E extends OrdinalValue<E>> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Build instances of {@link ImmutableOrdinalSet}.
   * @param <E> element type
   */
  public static class Builder<E extends OrdinalValue<E>> {
    private final List<E> builder = Lists.newArrayListWithExpectedSize(4);

    /**
     * Adds add elements from the iterable.
     * @param elements the elements
     * @return {@code this} builder for chained invocation
     */
    public Builder<E> addAll(Iterable<E> elements) {
      Iterables.addAll(builder, checkNotNull(elements));
      return this;
    }

    /**
     * Adds element.
     * @param element the element
     * @return {@code this} builder for chained invocation
     */
    public Builder<E> add(E element) {
      builder.add(checkNotNull(element));
      return this;
    }

    /**
     * Builds instances of {@link ImmutableOrdinalSet} using all added elements.
     * @return immutable ordinal set
     */
    public ImmutableOrdinalSet<E> build() {
      return ImmutableOrdinalSet.copyOf(builder);
    }
  }
}
