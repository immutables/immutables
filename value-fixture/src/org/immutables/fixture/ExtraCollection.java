/*
    Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.fixture;

import com.google.common.collect.*;
import org.immutables.value.Value;

import java.util.Arrays;

@Value.Immutable(singleton = true, prehash = true)
public interface ExtraCollection {
  @Value.Parameter
  Multiset<String> bag();

  @Value.Parameter
  Multimap<Integer, String> index();

  @Value.Parameter
  ListMultimap<Integer, String> indexList();

  @Value.Parameter
  SetMultimap<Integer, String> indexSet();

  default void use() {
    ImmutableExtraCollection.of(
        ImmutableList.<String>of(),
        ImmutableMultimap.<Integer, String>of(),
        ImmutableMultimap.<Integer, String>of(),
        ImmutableMultimap.<Integer, String>of());
    ImmutableExtraCollection.of();
    ImmutableExtraCollection collection = ImmutableExtraCollection.builder()
        .addBag("2", "2")
        .putIndex(1, "2", "3", "4")
        .putAllIndex(1, Arrays.asList("2", "3", "4"))
        .putIndex(2, "5")
        .putIndexList(1, "")
        .putIndexSet(2, "2")
        .putAllIndexSet(2, Arrays.asList("3", "4"))
        .build();

    collection.bag().count("2");
    collection.index().get(1);
    collection.indexList().get(1);
    collection.indexSet().get(2);
  }
}
