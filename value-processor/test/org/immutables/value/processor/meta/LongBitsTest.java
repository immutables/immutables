/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import com.google.common.collect.ImmutableList;
import org.immutables.value.processor.meta.LongBits.LongPositions;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class LongBitsTest {
  private static final String A = "a", B = "b", C = "c";
  private final LongBits longBits = new LongBits();

  @Test
  public void singleLong() {
    LongPositions positions = longBits.forIterable(ImmutableList.of(A, B), 2);
    check(positions.longsIndeces()).isOf(0);
    check(positions.apply(A).index).is(0);
    check(positions.apply(A).bit).is(0);
    check(positions.apply(A).mask).is(0b1L);

    check(positions.apply(B).index).is(0);
    check(positions.apply(B).bit).is(1);
    check(positions.apply(B).mask).is(0b10L);
  }

  @Test
  public void doubleLong() {
    LongPositions positions = longBits.forIterable(ImmutableList.of(A, B, C), 2);
    check(positions.longsIndeces()).isOf(0, 1);
    check(positions.apply(A).index).is(0);
    check(positions.apply(A).bit).is(0);
    check(positions.apply(A).mask).is(0b1L);

    check(positions.apply(B).index).is(0);
    check(positions.apply(B).bit).is(1);
    check(positions.apply(B).mask).is(0b10L);

    check(positions.apply(C).index).is(1);
    check(positions.apply(C).bit).is(0);
    check(positions.apply(C).mask).is(0b1L);
  }
}
