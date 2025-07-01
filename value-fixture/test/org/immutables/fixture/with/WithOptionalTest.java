package org.immutables.fixture.with;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class WithOptionalTest {
  @Test void returnsSame() {
    ImmutableWitherOptional wo = ImmutableWitherOptional.builder()
        .aa(ImmutableAnImm.of(1))
        .bb(ImmutableAnImm.of(2))
        .cc(Optional.of(ImmutableAnImm.of(3)))
        .build();

    ImmutableWitherOptional copied = wo.withBb(Optional.of(ImmutableAnImm.of(2)));
    // because of reference check
    check(copied != wo);

    copied = wo.withCc(Optional.of(ImmutableAnImm.of(3)));
    // because of equality check on suppressed optional
    check(copied).same(wo);
  }
}
