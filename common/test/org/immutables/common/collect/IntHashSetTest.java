/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.common.collect;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class IntHashSetTest {
  @Test
  public void addContainsRemove() {
    IntHashSet set = IntHashSet.withExpectedSize(4);

    check(set.add(1));
    check(set.size()).is(1);
    check(set.add(2));
    check(set.add(100));
    check(set.size()).is(3);

    check(!set.add(1));
    check(!set.add(100));

    check(set.size()).is(3);
    check(set.remove(1));
    check(set.size()).is(2);
    check(!set.remove(111));
    check(set.size()).is(2);

    check(set.contains(2));
    check(set.contains(100));

    check(!set.contains(3));
    check(!set.contains(4));
  }

  @Test
  public void resizing() {
    IntHashSet set = IntHashSet.withExpectedSize(1);

    fillSet(set);
    checkSet(set);
  }

  private void checkSet(IntHashSet set) {
    for (int i = 0; i < hashCodes.length; i++) {
      check(set.contains(hashCodes[i]));
    }
  }

  private void fillSet(IntHashSet set) {
    for (int i = 0; i < hashCodes.length; i++) {
      check(set.add(hashCodes[i]));
    }
  }

  int[] hashCodes = {
      973811913,
      1771137665,
      1448877707,
      937062374,
      1497481327,
      1722154764,
      115589528,
      572710495,
      494807572,
      1249825633,
      1761677726,
      1470019996,
      656046291,
      1392223290,
      1643803952,
      1963680242,
      1240409673,
      505154242,
      68063704,
      984622947,
      784879693,
      34319412,
      1818291377,
      1157223368,
      1987659998,
      806586641,
      1180615186,
      1280077210,
      426115611,
      654231037,
      1108633189,
      435297629,
      1024456811,
      231532599,
      1625203221,
      659734441,
      959208636,
      2029068211,
      703109797,
      260984278,
      421748969,
      1383979222,
      1293810398 };

}
