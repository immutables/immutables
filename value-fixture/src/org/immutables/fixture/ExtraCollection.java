package org.immutables.fixture;

import java.util.Arrays;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import org.immutables.value.Value;

@Value.Immutable
public interface ExtraCollection {
  Multiset<String> bag();

  Multimap<Integer, String> index();

  ListMultimap<Integer, String> indexList();

  SetMultimap<Integer, String> indexSet();

  default void use() {
    ImmutableExtraCollection collection = ImmutableExtraCollection.builder()
        .addBag("2", "2")
        .putAllIndex(1, "2", "3", "4")
        .putAllIndex(1, Arrays.asList("2", "3", "4"))
        .putIndex(2, "5")
        .putIndexList(1, "")
        .putAllIndexSet(2, "2")
        .build();

    collection.bag().count("2");
    collection.index().get(1);
    collection.indexList().get(1);
    collection.indexSet().get(2);
  }
}
