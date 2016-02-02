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
package org.immutables.fixture.modifiable;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ClearBuilderTest {
  @Test
  public void clear() {
    ImmutableClearBuilder.Builder builder = ImmutableClearBuilder.builder()
        .a(true)
        .b("aaa")
        .addL("1")
        .putM("2", 2);

    ImmutableClearBuilder b1 = builder.build();

    builder.clear();

    try {
      builder.build();
      check(false); // cannot build, state was reset
    } catch (IllegalStateException ex) {
    }

    ImmutableClearBuilder b2 = builder
        .a(false)
        .b("bbb")
        .build();

    check(b2).not(b1);

    ImmutableClearBuilder b3 = builder.addL("2").build();

    check(!b3.a());
    check(b3.b()).is("bbb");
    check(b3.l()).isOf("2");
  }
}
