/*
    Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.builder.fixture;

import org.immutables.builder.fixture.Factory1Builder;
import org.immutables.builder.fixture.Factory2Builder;
import org.immutables.builder.fixture.Factory3Builder;
import org.immutables.builder.fixture.Factory4Builder;
import org.immutables.builder.fixture.Factory5Builder;
import java.lang.annotation.RetentionPolicy;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class FactoryParametersAndSwitcherTest {

  @Test
  public void parameters() {

    check(new Factory1Builder(null)
        .theory(1)
        .reality("1")
        .build()).is("1 != 1, null");

    check(new Factory2Builder(2, "2").build()).is("2 != 2");

    check(Factory3Builder.newBuilder(3).reality("3").build()).is("3 != 3");
  }

  @Test
  public void switcher() {

    check(Factory4Builder.newBuilder(4)
        .runtimePolicy()
        .sourcePolicy()
        .classPolicy()
        .build()).is("" + RetentionPolicy.CLASS + 4);

    check(Factory4Builder.newBuilder(42)
        .sourcePolicy()
        .runtimePolicy()
        .build()).is("" + RetentionPolicy.RUNTIME + 42);

    try {
      Factory4Builder.newBuilder(44).build();
      check(false);
    } catch (IllegalStateException ex) {
    }
  }

  @Test
  public void switcherDefaults() {
    check(Factory5Builder.newBuilder().build()).is("" + RetentionPolicy.SOURCE);
    check(Factory5Builder.newBuilder().runtimePolicy().build()).is("" + RetentionPolicy.RUNTIME);
  }

  @Test
  public void strictSwitches() {
    // cannot init twice on switcher
    try {
      new Factory7Builder(22)
          .runtimePolicy()
          .sourcePolicy()
          .build();
      check(false);
    } catch (IllegalStateException ex) {
    }

    // cannot init twice
    try {
      new Factory6Builder(22)
          .reality("1")
          .reality("2")
          .build();
      check(false);
    } catch (IllegalStateException ex) {
    }

    // defaults still works
    check(new Factory7Builder(1).build()).is(RetentionPolicy.CLASS + "1");
  }
}
