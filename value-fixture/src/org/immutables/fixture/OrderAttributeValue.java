package org.immutables.fixture;

import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
public abstract class OrderAttributeValue {
  @Value.Order.Natural
  public abstract Set<Integer> natural();

  @Value.Order.Reverse
  public abstract Set<String> reverse();
}
