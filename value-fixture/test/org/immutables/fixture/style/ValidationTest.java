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
package org.immutables.fixture.style;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class ValidationTest {
  @Test
  public void noValidation() {
    ImmutableNoValidation nv = ImmutableNoValidation.builder()
        .a(null).build();

    check(nv.a()).isNull();
    check(nv.b()).isNull();
    check(!nv.z());
    check(nv.i()).is(0);

    nv = ImmutableNoValidation.of(null, null, true, 0);
    check(nv.a()).isNull();
    check(nv.b()).isNull();
    check(nv.z());

    nv = nv.withA(null).withB(null);

    check(nv.a()).isNull();
    check(nv.b()).isNull();
  }

  @Test
  public void noValidationInJdkCollections() {
    ImmutableNoValidation nv = ImmutableNoValidation.builder().build();

    check(nv.withSet((String) null).set()).isOf((String) null);
    check(nv.withList((String) null).list()).isOf((String) null);
    check(nv.withMap(Collections.singletonMap("a", null)).map()).is(Collections.singletonMap("a", null));

    nv = ImmutableNoValidation.builder()
        .addSet((String) null)
        .addList((String) null)
        .putAllMap(Collections.singletonMap("b", null))
        .putMap(null, null)
        .build();

    check(nv.set()).isOf((String) null);
    check(nv.list()).isOf((String) null);
    check(nv.map().values()).isOf(null, null);
  }
}
