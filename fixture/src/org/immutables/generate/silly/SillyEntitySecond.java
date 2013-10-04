package org.immutables.generate.silly;

import org.immutables.annotation.GenerateDefault;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaled;
import org.immutables.annotation.GenerateRepository;
import org.immutables.common.repository.Id;

@GenerateImmutable
@GenerateRepository("ent2")
public abstract class SillyEntitySecond {

  @GenerateMarshaled("_id")
  @GenerateDefault
  public Id id() {
    return Id.generate();
  }
}
