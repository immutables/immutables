package org.immutables.fixture.modifiable;

import com.google.common.base.Optional;
import com.google.common.collect.*;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

@Value.Immutable
@Value.Modifiable
interface Companion {
  int integer();

  String string();

  @Nullable
  Boolean bools();

  List<String> str();

  Set<Integer> ints();

  int[] arrayInts();

  String[] arrayStrings();

  @Value.NaturalOrder
  SortedSet<Integer> ords();

  Set<RetentionPolicy> pols();

  @Value.ReverseOrder
  NavigableSet<Integer> navs();

  Map<Long, Integer> just();

  @Value.NaturalOrder
  SortedMap<Integer, String> ordsmap();

  Map<RetentionPolicy, Integer> polsmap();

  @Value.ReverseOrder
  NavigableMap<String, Integer> navsmap();

  @Value.Modifiable
  @Value.Style(create = "new")
  interface Small {
    @Value.Parameter
    int first();

    @Value.Parameter
    String second();
  }

  @Value.Modifiable
  interface Standalone {
    @Value.Parameter
    int first();

    @Value.Parameter
    String second();

    @Value.Parameter
    short sh();

    @Value.Parameter
    char ch();

    @Value.Parameter
    boolean bool();

    @Value.Parameter
    double dob();

    @Value.Parameter
    float fl();

    Optional<Integer> v1();

    java.util.Optional<Integer> v2();

    OptionalInt i1();

    OptionalLong l1();

    OptionalDouble d1();

    @Value.Default
    default int def() {
      return 1;
    }

    @Value.Lazy
    default String lazy() {
      return "";
    }

    @Value.Derived
    default int derived() {
      return v1().or(0);
    }
  }

  @Value.Modifiable
  interface Extra {
    @Value.Parameter
    Multiset<String> bag();

    @Value.Parameter
    Multimap<Integer, String> index();

    @Value.Parameter
    ListMultimap<Integer, String> indexList();

    @Value.Parameter
    SetMultimap<Integer, String> indexSet();

    @Value.Parameter
    BiMap<Integer, String> biMap();
  }

  @Value.Modifiable
  @Value.Immutable
  @Value.Style(jdkOnly = true)
  interface JdkComp {
    int integer();

    String string();

    @Nullable
    Boolean bools();

    List<String> str();

    Set<Integer> ints();

    int[] arrayInts();

    String[] arrayStrings();

    @Value.NaturalOrder
    SortedSet<Integer> ords();

    Set<RetentionPolicy> pols();

    @Value.ReverseOrder
    NavigableSet<Integer> navs();
  }
}
