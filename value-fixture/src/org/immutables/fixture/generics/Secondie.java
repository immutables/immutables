package org.immutables.fixture.generics;

import com.google.common.collect.Multimap;
import java.util.Map;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
public interface Secondie<T, V> {

  @Value.Parameter
  @Nullable
  V integer();

  @Value.Parameter
  @Nullable
  List<T> list();

  @Value.Default
  @Nullable
  default Set<V> set() {
    return Collections.emptySet();
  }

  Multimap<T, V> coll();

  @Nullable
  @Value.Derived
  default List<V> lst() {
    return Collections.singletonList(integer());
  }
}
