package org.immutables.fixture.generics;

import com.google.common.collect.Multimap;
import org.immutables.value.Value;

@Value.Enclosing
public interface MultimapSafeVarargs {
  @Value.Immutable
  interface ParamKey {
    Multimap<Val<String>, String> vals();
  }
  @Value.Immutable
  interface ParamVal {
    Multimap<String, Val<String>> vals();
  }
  @Value.Immutable
  interface ParamBoth {
    Multimap<Val<String>, Val<String>> vals();
  }

  interface Val<T> {}
}
