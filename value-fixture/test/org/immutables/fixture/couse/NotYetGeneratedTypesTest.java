package org.immutables.fixture.couse;

import static org.immutables.check.Checkers.check;
import org.immutables.fixture.couse.ImmutableEnumUser;
import org.immutables.fixture.couse.ImmutableHasEnum;
import org.immutables.fixture.couse.sub.ImmutableHasEnumOtherPackage;
import org.junit.Test;

public class NotYetGeneratedTypesTest {
  @Test
  public void nestedEnums() {
    ImmutableEnumUser user = ImmutableEnumUser.builder()
        .type(ImmutableHasEnum.Type.FOO)
        .type2(ImmutableHasEnumOtherPackage.Type.BAR)
        .build();

    check(user.getType()).is(ImmutableHasEnum.Type.FOO);
    check(user.getType2()).is(ImmutableHasEnumOtherPackage.Type.BAR);
  }
}
