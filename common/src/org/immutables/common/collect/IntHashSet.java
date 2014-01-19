/*
    Copyright 2013-2014 Immutables.org authors

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
import com.google.common.collect.ImmutableSet;
import javax.annotation.concurrent.NotThreadSafe;
import static com.google.common.base.Preconditions.*;

/**
 * Primitive integer hash set, designed to store good quality hashes (do not use implicit
 * "smearing"). Set is mutable, not thread safe and support only addition of values, checks for
 * containment, and removals. Equality of set itself is not supported as well.
 * <p>
 * <em>java.util.Set is not implemented by IntHashSet because of many differences and omissions in functionality</em>
 * <p>
 * <strong>Warning! Zero '0' values not supported, because it is reserved, if zero values are
 * possible, then mask them with some other value</strong>
 */
@Beta
@NotThreadSafe
public final class IntHashSet {

  private static final String RESERVED_VALUE_MESSAGE =
      "0 is reserved value and cannot be used. If needed, mask it with different non-zero value";

  private static final int LOWER_CAPACITY = 4;
  private static final int UPPER_CAPACITY = 1 << 29;

  private static final int SMEAR_C1 = 0xcc9e2d51;
  private static final int SMEAR_C2 = 0x1b873593;

  private int[] table;
  private int size;
  private int limit;

  /*
   * This method was rewritten in Java from an intermediate step of the Murmur hash function in
   * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp, which contained the
   * following header:
   *
   * MurmurHash3 was written by Austin Appleby, and is placed in the public domain. The author
   * hereby disclaims copyright to this source code.
   */
  public static int smear(int hashCode) {
    return SMEAR_C2 * Integer.rotateLeft(hashCode * SMEAR_C1, 15);
  }

  /**
   * Constructs new set with expected size, to work sufficiently fast and avoiding
   * unnecessary rehashing.
   * @param expectedSize the expected number of unique value
   * @return the new empty set instance
   */
  public static IntHashSet withExpectedSize(int expectedSize) {
    return new IntHashSet(expectedSize);
  }

  private IntHashSet(int expectedSize) {
    init(capacity(expectedSize));
  }

  /**
   * The size of a set
   * @return size of a set
   */
  public int size() {
    return size;
  }

  /**
   * Initializes the hash table array with capacity and limit of elements for next resize.
   * @param capacity the determined capacity
   */
  private void init(int capacity) {
    this.limit = (capacity * 2) / 3;
    this.table = new int[capacity];
  }

  /**
   * Calculates capacity suitable for expected size of set
   * @param expectedSize the expected size
   * @return the optimal capacity
   */
  private int capacity(int expectedSize) {
    int capacity = (3 * expectedSize) / 2;

    if (capacity < 0 || capacity > UPPER_CAPACITY) {
      return UPPER_CAPACITY;
    }

    int c = LOWER_CAPACITY;
    while (c < capacity) {
      c <<= 1;
    }

    return c;
  }

  /**
   * Rebuilds hash table,
   * @param expectedSize the expected size
   */
  private void rebuildHash(int expectedSize) {
    int[] t = this.table;
    init(capacity(expectedSize));

    for (int i = 0; i < t.length; i++) {
      int v = t[i];
      if (v != 0) {
        add(v);
      }
    }
  }

  /**
   * Removes value to set, no rehashing will ever occur
   * @param value the value to remove
   * @return true, if value existed before and was removed
   */
  public boolean remove(int value) {
    checkArgument(value != 0, RESERVED_VALUE_MESSAGE);
    int l = table.length;
    for (int i = address(value, l);;) {
      int v = table[i];
      if (v == 0) {
        return false;
      }
      if (v == value) {
        table[i] = 0;
        size--;
        return true;
      }
      if (++i == l) {
        i = 0;
      }
    }
  }

  /**
   * Add value to set, rebuilding hash-table when limit 2/3 of capacity reached.
   * @param value the value
   * @return true, if value was not added before, false otherwise
   */
  public boolean add(int value) {
    checkArgument(value != 0, RESERVED_VALUE_MESSAGE);
    int l = table.length;
    for (int i = address(value, l);;) {
      int v = table[i];
      if (v == 0) {
        table[i] = value;
        break;
      }
      if (v == value) {
        return false;
      }
      if (++i == l) {
        i = 0;
      }
    }
    if (++size >= limit) {
      rebuildHash(l);
    }
    return true;
  }

  /**
   * Checks if this set contains a value.
   * @param value the value
   * @return true, if contains
   */
  public boolean contains(int value) {
    int l = table.length;
    for (int i = address(value, l);;) {
      int v = table[i];
      if (v == 0) {
        return false;
      }
      if (v == value) {
        return true;
      }
      if (++i == l) {
        i = 0;
      }
    }
  }

  /**
   * converts this integer has set to {@link ImmutableSet}
   * @return immutable set of {@link Integer}
   */
  public ImmutableSet<Integer> toSet() {
    ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
    for (int i = 0; i < table.length; i++) {
      int v = table[i];
      if (v != 0) {
        builder.add(v);
      }
    }
    return builder.build();
  }

  /**
   * Open addressing function provides starting offset for the hash-table.
   * @param value value to hash
   * @param length
   * @return offset in table to start search
   */
  private static int address(int value, int length) {
    return ((value << 1) - (value << 8)) & (length - 1);
  }

  /**
   * Output all values as string
   */
  @Override
  public String toString() {
    return toSet().toString();
  }
}
