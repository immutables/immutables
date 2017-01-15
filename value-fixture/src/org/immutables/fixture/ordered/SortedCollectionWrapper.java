package org.immutables.fixture.ordered;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableSortedSet;
import org.immutables.value.Value;

@Value.Immutable
public interface SortedCollectionWrapper {
    @Value.NaturalOrder
    ImmutableSortedSet<Elem> getElemSet();

    @Value.NaturalOrder
    ImmutableSortedSet<ImmutableElem> getImmutableElemSet();

    @Value.ReverseOrder
    ImmutableSortedMultiset<Elem> getElemMultiset();

    @Value.ReverseOrder
    ImmutableSortedMultiset<ImmutableElem> getImmutableElemMultiset();

    @Value.NaturalOrder
    ImmutableSortedMap<Elem, Void> getElemMap();

    @Value.NaturalOrder
    ImmutableSortedMap<ImmutableElem, Void> getImmutableElemMap();

    @Value.Immutable
    public interface Elem extends Comparable<Elem> {
        int getValue();

        @Override
        public default int compareTo(Elem other) {
            return getValue() - other.getValue();
        }
    }
}
