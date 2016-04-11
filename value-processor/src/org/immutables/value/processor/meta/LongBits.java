/*
   Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.primitives.Longs;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Set;
import static com.google.common.base.Preconditions.*;

/**
 * Structure to calculate bit packing
 */
public final class LongBits implements Function<Iterable<? extends Object>, LongBits.LongPositions> {
  private static final int BITS_IN_LONG = Longs.BYTES * Byte.SIZE;

  @Override
  public LongPositions apply(Iterable<? extends Object> input) {
    return forIterable(input, BITS_IN_LONG);
  }

  public LongPositions forIterable(Iterable<? extends Object> input, int bitPerLong) {
    return new LongPositions(input, bitPerLong);
  }

  public static final class LongPositions implements Function<Object, BitPosition> {
    private final IdentityHashMap<Object, BitPosition> positions = Maps.newIdentityHashMap();
    private final ImmutableList<Object> elements;
    private final ImmutableMap<Integer, LongSet> longPositions;

    LongPositions(Iterable<? extends Object> elements, final int bitPerLong) {
      this.elements = ImmutableList.copyOf(elements);
      checkArgument(bitPerLong <= BITS_IN_LONG, bitPerLong);

      for (int i = 0; i < this.elements.size(); i++) {
        positions.put(
            this.elements.get(i),
            new BitPosition(
                i / bitPerLong,
                i % bitPerLong));
      }

      this.longPositions = ImmutableSortedMap.copyOf(
          Maps.transformEntries(
              Multimaps.index(positions.values(), ToLongIndex.FUNCTION).asMap(),
              new Maps.EntryTransformer<Integer, Collection<BitPosition>, LongSet>() {
                @Override
                public LongSet transformEntry(Integer key, Collection<BitPosition> position) {
                  return new LongSet(key, position);
                }
              }));
    }

    public Set<Integer> longsIndeces() {
      return longPositions.keySet();
    }

    public Collection<LongSet> longs() {
      return longPositions.values();
    }

    @Nullable
    @Override
    public BitPosition apply(Object input) {
      return positions.get(input);
    }
  }

  public static final class LongSet {
    public final int index;
    public final int occupation;
    public final Iterable<BitPosition> positions;

    LongSet(int index, Iterable<BitPosition> positions) {
      this.index = index;
      this.positions = ImmutableList.copyOf(positions);
      this.occupation = computeOccupation();
    }

    private int computeOccupation() {
      int occupation = 0;
      for (BitPosition position : this.positions) {
        occupation |= position.mask;
      }
      return occupation;
    }
  }

  enum ToLongIndex implements Function<BitPosition, Integer> {
    FUNCTION;
    @Override
    public Integer apply(BitPosition input) {
      return input.index;
    }
  }

  public static final class BitPosition {
    public final int index;
    public final int bit;
    public final long mask;

    BitPosition(int index, int bit) {
      this.index = index;
      this.bit = bit;
      this.mask = 1L << bit;
    }
  }
}
