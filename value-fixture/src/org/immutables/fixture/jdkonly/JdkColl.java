package org.immutables.fixture.jdkonly;

import java.util.NavigableSet;
import java.lang.annotation.RetentionPolicy;
import java.util.SortedSet;
import java.util.Set;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable(singleton = true, jdkOnly = true)
public interface JdkColl {
  List<String> str();

  Set<Integer> ints();

  @Value.NaturalOrder
  SortedSet<Integer> ords();

  Set<RetentionPolicy> pols();

  @Value.ReverseOrder
  NavigableSet<Integer> navs();
}
