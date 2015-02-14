package org.immutables.fixture;

import org.immutables.value.Value;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.lang.annotation.RetentionPolicy;

@Value.Immutable
public interface UseImmutableCollections {
  ImmutableList<String> list();

  ImmutableSet<Integer> set();

  ImmutableSet<RetentionPolicy> enumSet();

  @Value.NaturalOrder
  ImmutableSortedSet<RetentionPolicy> sortedSet();

  ImmutableMultiset<String> multiset();

  ImmutableMap<String, Integer> map();

  ImmutableMap<RetentionPolicy, Integer> enumMap();

  @Value.ReverseOrder
  ImmutableSortedMap<RetentionPolicy, Integer> sortedMap();

  ImmutableMultimap<String, Integer> multimap();

  ImmutableSetMultimap<String, Integer> setMultimap();

  ImmutableListMultimap<String, Integer> listMultimap();

  default void use() {

    ImmutableUseImmutableCollections.builder()
        .addList("a", "b")
        .addSet(1, 2)
        .addEnumSet(RetentionPolicy.CLASS)
        .addSortedSet(RetentionPolicy.RUNTIME)
        .addMultiset("c", "c")
        .putMap("d", 1)
        .putEnumMap(RetentionPolicy.RUNTIME, 2)
        .putSortedMap(RetentionPolicy.SOURCE, 3)
        .putMultimap("e", 2)
        .putSetMultimap("f", 5)
        .putListMultimap("g", 6)
        .build();
  }
}
