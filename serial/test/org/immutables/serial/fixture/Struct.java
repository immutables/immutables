package org.immutables.serial.fixture;

import java.util.Set;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.List;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Serial.Version(2L)
@Serial.Structural
@Value.Immutable(builder = false)
interface StructFromConstructor {
  @Value.Parameter
  int a();

  @Value.Parameter
  List<Integer> c();

  @Value.Parameter
  Optional<String> os();
  @Value.Parameter
  Multiset<String> bag();

  @Value.Parameter
  Multimap<Integer, String> index();

  @Value.Parameter
  ListMultimap<Integer, String> indexList();

  SetMultimap<Integer, String> indexSet();

  @Value.Parameter
  BiMap<Integer, String> biMap();
}

@Serial.Version(3L)
@Serial.Structural
@Value.Immutable
interface StructFromBuilder {
  int a();

  String s();
  boolean[] array();

  Set<String> c();

  Optional<Boolean> os();
  Multiset<String> bag();
  Multimap<Integer, String> index();
  ListMultimap<Integer, String> indexList();
  SetMultimap<Integer, String> indexSet();
  BiMap<Integer, String> biMap();
}
