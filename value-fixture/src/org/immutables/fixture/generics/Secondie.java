package org.immutables.fixture.generics;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

public interface Secondie<T, V> {

  @Nullable
  Integer integer();

  @Nullable
  List<T> list();

  @Value.Default
  @Nullable
  default Set<V> set() {
    return Collections.emptySet();
  }
}
