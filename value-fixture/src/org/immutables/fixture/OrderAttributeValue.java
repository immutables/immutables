package org.immutables.fixture;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.Map;
import org.immutables.value.Value;
import java.util.NavigableSet;
import java.util.Set;

@Value.Immutable
public abstract class OrderAttributeValue {
  @Value.Order.Natural
  public abstract Set<Integer> natural();

  @Value.Order.Reverse
  public abstract NavigableSet<String> reverse();

  @Value.Order.Natural
  public abstract Map<Integer, String> naturalMap();

  @Value.Order.Reverse
  public abstract SortedMap<String, String> reverseMap();

  @Value.Order.Natural
  public abstract NavigableMap<String, String> navigableMap();
}
