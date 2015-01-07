package org.immutables.fixture.jdkonly;

import org.immutables.value.Value.Immutable.ImplementationVisibility;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import org.immutables.value.Value;

@Value.Immutable(visibility = ImplementationVisibility.PRIVATE, jdkOnly = true)
public interface JdkMaps {
  Map<Long, Integer> just();

  @Value.NaturalOrder
  SortedMap<Integer, String> ords();

  Map<RetentionPolicy, Integer> pols();

  @Value.ReverseOrder
  NavigableMap<String, Integer> navs();
}
