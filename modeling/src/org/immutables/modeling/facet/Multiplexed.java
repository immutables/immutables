package org.immutables.modeling.facet;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Defines adaptable/multiplexed target type for facets.
 */
public abstract class Multiplexed {

  private final List<Object> contextualObjects = Lists.newArrayListWithExpectedSize(2);
  {
    contextualObjects.add(this);
  }

  public final void addContextual(Object contextual) {
    // contextual;
  }

  @Nullable
  public final <T> T as(Class<T> type) {
    return findInstanceOf(type);
  }

  @Nullable
  private <T> T findInstanceOf(Class<T> type) {
    for (Object object : contextualObjects) {
      if (type.isInstance(object)) {
        return type.cast(object);
      }
    }
    return null;
  }
}
