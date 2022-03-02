package org.immutables.fixture;

import java.io.Serializable;
import org.immutables.value.Value;

@Value.Immutable(lazyhash = true)
public interface LazyHashSerializable extends Serializable {
  String s();
  boolean b();
  int i();
}
