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
package org.immutables.fixture.nested;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class BaseFromTest {
  @Test
  public void from() {
    BaseFrom baseFrom = new BaseFrom() {
      @Override
      public boolean isA() {
        return false;
      }

      @Override
      public String getB() {
        return "";
      }
    };
    ImmutableSub sub = ImmutableSub.builder()
        .from(baseFrom)
        .c("*")
        .build();

    check(sub.getB()).is("");
    check(sub.getC()).is("*");
    check(!sub.isA());
  }

  @Test
  public void complicatedFrom() {
    ImmutableAB ab = ImmutableAB.builder()
        .a(1)
        .addB("a", "b")
        .c(3)
        .build();

    ImmutableAB copiedAb = ImmutableAB.builder()
        .from(ab)
        .build();

    check(copiedAb).is(ab);
  }
}
