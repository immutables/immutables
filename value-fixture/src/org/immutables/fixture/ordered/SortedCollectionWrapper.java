/*
   Copyright 2016 Immutables Authors and Contributors

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
