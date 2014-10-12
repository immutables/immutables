package org.immutables.generate.silly;

import org.immutables.annotation.GenerateMarshaled;
import org.immutables.annotation.GenerateRepository;
import org.immutables.common.repository.Id;
import org.immutables.value.Value;

@Value.Immutable
@GenerateRepository("ent2")
public abstract class SillyEntitySecond {

  @GenerateMarshaled("_id")
  @Value.Default
  public Id id() {
    return Id.generate();
  }
}
