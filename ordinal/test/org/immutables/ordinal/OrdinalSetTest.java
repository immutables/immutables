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
package org.immutables.ordinal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class OrdinalSetTest {

  Domain da = new Domain();
  Domain db = new Domain();

  Ord a0 = da.get(0);
  Ord a1 = da.get(1);

  Ord b0 = db.get(0);
  Ord b1 = db.get(1);

  // not using IterableChecker in all cases (to cover correct code paths)
  @Test
  public void emptySet() {
    check(ImmutableOrdinalSet.<Ord>of().isEmpty());
    check(ImmutableOrdinalSet.<Ord>of().size()).is(0);

    check(ImmutableOrdinalSet.<Ord>of()).same(ImmutableOrdinalSet.<Ord>of());
    check(ImmutableOrdinalSet.<Ord>of()).asString().notEmpty();
  }

  @Test
  public void builder() {
    check(ImmutableOrdinalSet.<Ord>builder()
        .add(a0)
        .addAll(ImmutableList.of(a1))
        .build())
        .is(ImmutableOrdinalSet.of(a0, a1));
  }

  @Test
  public void singletonSet() {
    check(!ImmutableOrdinalSet.of(a0).isEmpty());
    check(ImmutableOrdinalSet.of(a0).size()).is(1);
    check(ImmutableOrdinalSet.of(a0)).asString().notEmpty();

    check(ImmutableOrdinalSet.of(a0)).isOf(a0);
    check(ImmutableOrdinalSet.of(a0)).has(a0);
    check(ImmutableOrdinalSet.of(a0).contains(a0));
    check(!ImmutableOrdinalSet.of(a0).contains(a1));
    check(ImmutableOrdinalSet.of(a0)).not(ImmutableOrdinalSet.of(da.get(4)));
    check(ImmutableOrdinalSet.of(a0).containsAll(ImmutableOrdinalSet.of(a0)));
    check(ImmutableOrdinalSet.of(a0).containsAll(ImmutableSet.of(a0)));
    check(ImmutableOrdinalSet.of(a0)).not().hasAll(ImmutableSet.of(da.get(5)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void differentDomainInRegularSet() {
    ImmutableOrdinalSet.of(a0, b0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicateInRegularSet() {
    ImmutableOrdinalSet.of(a0, a0);
  }

  @Test
  public void copyOfIterable() {
    ImmutableOrdinalSet<Ord> s1 = ImmutableOrdinalSet.of(a1);
    check(ImmutableOrdinalSet.copyOf(s1)).same(s1);
    check(ImmutableOrdinalSet.copyOf(Arrays.<Ord>asList())).same(ImmutableOrdinalSet.<Ord>of());
    check(ImmutableOrdinalSet.copyOf(Arrays.asList(b0))).isOf(b0);
    check(ImmutableOrdinalSet.copyOf(Arrays.asList(a0, a1))).isOf(a0, a1);
  }

  @Test
  public void regularSetBasic() {
    check(!ImmutableOrdinalSet.of(a0, a1).isEmpty());
    check(ImmutableOrdinalSet.of(b0, b1).size()).is(2);
  }

  @Test
  public void incrementCountersSmall() {
    int[] counters = new int[da.length()];

    ImmutableOrdinalSet.of(a1).incrementCounters(counters);
    ImmutableOrdinalSet.of(a0).incrementCounters(counters);
    ImmutableOrdinalSet.of(a1).incrementCounters(counters);
    ImmutableOrdinalSet.of().incrementCounters(counters);

    check(counters).isOf(1, 2);
  }

  @Test
  public void incrementCountersLarge() {
    Domain dc = new Domain();
    Ord[] cs = new Ord[120];
    for (int i = 0; i < cs.length; i++) {
      cs[i] = dc.get(i);
    }

    int[] counters = new int[dc.length()];
    ImmutableOrdinalSet.of(cs[62], cs[119], cs[98]).incrementCounters(counters);
    ImmutableOrdinalSet.of(cs[119], cs[1]).incrementCounters(counters);

    check(new int[] { counters[0], counters[1], counters[62], counters[98], counters[119] })
        .isOf(0, 1, 1, 1, 2);
  }

  @Test
  public void setContains() {
    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableOrdinalSet.of(b0)));
    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableOrdinalSet.of(b0, b1)));

    check(ImmutableOrdinalSet.of(b0, b1).containsAny(ImmutableOrdinalSet.of(b0)));
    check(ImmutableOrdinalSet.of(b1).containsAny(ImmutableOrdinalSet.of(b0, b1)));
    check(ImmutableOrdinalSet.of(b0, b1).containsAny(ImmutableOrdinalSet.of(b0, b1)));
    check(!ImmutableOrdinalSet.of().containsAny(ImmutableOrdinalSet.of(b0, b1)));

    check(!ImmutableOrdinalSet.of(b0, b1)
        .containsAll(ImmutableOrdinalSet.of(a0, a1)));

    check(!ImmutableOrdinalSet.of(b0, b1)
        .containsAll(ImmutableOrdinalSet.of(db.get(120), db.get(130))));

    check(!ImmutableOrdinalSet.of(db.get(30), db.get(70))
        .containsAll(ImmutableOrdinalSet.of(db.get(30), db.get(60))));

    check(ImmutableOrdinalSet.of(b0, b1)).hasAll(b0);
    check(ImmutableOrdinalSet.of(b0, b1)).not().hasAll(a0, a1);
    check(ImmutableOrdinalSet.of(b0, b1)).not().hasAll(b0, a1);
    check(ImmutableOrdinalSet.of(b0, b1)).not().has(a1);

    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableSet.of()));
    check(ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableSet.of(b0, b1)));
    check(!ImmutableOrdinalSet.of(b0, b1).contains(a1));
    check(!ImmutableOrdinalSet.of(b0, b1).containsAll(ImmutableSet.of(a1, a0)));
  }
}
