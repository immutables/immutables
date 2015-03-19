package org.immutables.fixture.serial;

import org.immutables.value.Value;
import java.io.Serializable;

@Value.Immutable(intern = true)
public abstract class SomeSer implements Serializable {

  private static final long serialVersionUID = 1L;

  @Value.Default
  int regular() {
    return 0;
  }

  @Value.Lazy
  int version() {
    return 1;
  }

  @Value.Immutable(singleton = true)
  interface OthSer extends Serializable {}
}
