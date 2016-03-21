/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
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
