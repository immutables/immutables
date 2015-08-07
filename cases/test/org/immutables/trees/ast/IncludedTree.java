package org.immutables.trees.ast;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;

// Included in SampleTree
public interface IncludedTree {

  @Value.Immutable
  public interface Included1 {}

  @Value.Immutable
  public interface Included2 {
    Optional<Included1> included1();

    Optional<Integer> intOpt();

    Optional<String> stringOpt();

    Set<Integer> intSet();

    Multimap<Integer, Long> multimap();

    Map<String, Object> map();
  }
}
