package org.immutables.fixture.jdkonly;

import org.immutables.value.Value;
import com.google.common.base.Optional;

@Value.Immutable
public interface UsingBothOptionals {
  Optional<Integer> v1();

  java.util.Optional<Integer> v2();

  class Use {
    void use() {


    }
  }
}
