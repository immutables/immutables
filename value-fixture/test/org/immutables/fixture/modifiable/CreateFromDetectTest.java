package org.immutables.fixture.modifiable;

import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class CreateFromDetectTest {
  @Test
  public void wrapModifiable() {
    
    ModifiableAaa aaa = ModifiableAaa.create()
        .setBbb(ImmutableBbb.builder().zzz(1).build());
    
    check(aaa.bbb()).isA(ModifiableBbb.class);

  }
}
