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
package org.immutables.fixture;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SafeInitTest {
  @Test
  public void cycles() {
    try {
      ImmutableSafeInitIface.builder().build();
      check(false);
    } catch (IllegalStateException ex) {
      check(ex.getMessage()).contains("[b, a, c]");
    }
  }

  @Test
  public void resolved() {
    check(ImmutableSafeInitIface.builder().a(1).build().c()).is(1);
    check(ImmutableSafeInitIface.builder().b(2).build().a()).is(2);
    check(ImmutableSafeInitIface.builder().c(3).build().b()).is(3);
  }

  @Test
  public void singleton() {
    ImmutableSafeInitSingl singl = ImmutableSafeInitSingl.of();
    check(singl.a()).is(1);
    check(singl.b()).is(2);

    ImmutableSafeInitSingl nonSingleton = ImmutableSafeInitSingl.builder()
        .a(0)
        .build();

    check(nonSingleton.b()).is(1);
  }

  @Test
  public void unsafeOrdering() {
    ImmutableSafeInitAclass obj = ImmutableSafeInitAclass.builder()
        .c(1)
        .build();

    check(obj.a()).not(1);
    check(obj.b()).not(1);
  }

  @Test
  public void defaultAndDerived() {
    DefaultDerivedInit source = ImmutableDefaultDerivedInit.builder().title("a").build();
    DefaultDerivedInit replica = ImmutableDefaultDerivedInit.copyOf(source).withTitle("b");
    check(replica.index()).is(source.index());

    replica = ImmutableDefaultDerivedInit.of(source.uuid(), source.title());
    check(replica.index()).is(source.index());
  }
}
