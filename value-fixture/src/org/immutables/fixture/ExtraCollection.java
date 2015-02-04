package org.immutables.fixture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import java.util.Arrays;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
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
