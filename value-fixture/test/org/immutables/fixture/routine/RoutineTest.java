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
package org.immutables.fixture.routine;

import org.junit.Test;
import static org.immutables.check.Checkers.check;

// tests that Routines.immutableCopyOf is invoked on Aa init
public class RoutineTest {
  @Test
  public void normalize() {
    check(ImmutableBb.builder()
        .aa(new Aa.C())
        .bb(new Bb() {
          @Override
          public Aa aa() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Bb bb() {
            throw new UnsupportedOperationException();
          }
        })
        .build()
        .aa()).isA(Aa.D.class);
  }
}
