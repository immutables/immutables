/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.generate.silly;

import java.util.Arrays;
import org.immutables.common.collect.ImmutableOrdinalSet;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ValuesTest {
  
  @Test
  public void builderInheritence() {
    check(ImmutableSillyExtendedBuilder.builder().base);
  }

  @Test
  public void ordinalValue() {
    ImmutableSillyOrdinal a = ImmutableSillyOrdinal.of("a");
    ImmutableSillyOrdinal b = ImmutableSillyOrdinal.of("b");
    ImmutableSillyOrdinal c = ImmutableSillyOrdinal.of("c");

    check(Arrays.asList(a.ordinal(), b.ordinal(), c.ordinal())).isOf(0, 1, 2);

    check(a.ordinalDomain().get(1)).same(b);
    check(a.ordinalDomain().get(0)).same(a);

    check(a.ordinalDomain().length()).is(3);

    check(ImmutableSillyOrdinal.of("a")).same(a);
    check(ImmutableSillyOrdinal.of("b")).same(b);
  }

  @Test
  public void ordinalValueSet() {
    check(ImmutableSillyOrdinalHolder.builder()
        .addSet(ImmutableSillyOrdinal.of("a"))
        .build()
        .set())
        .isA(ImmutableOrdinalSet.class);
  }

  @Test
  public void internedInstanceConstruction() {
    check(ImmutableSillyInterned.of(1, 2)).is(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).same(ImmutableSillyInterned.of(1, 2));
    check(ImmutableSillyInterned.of(1, 2)).not(ImmutableSillyInterned.of(2, 2));

    check(ImmutableSillyInterned.builder()
        .arg1(1)
        .arg2(2)
        .build())
        .same(ImmutableSillyInterned.of(1, 2));

    check(ImmutableSillyInterned.of(1, 2).hashCode()).is(ImmutableSillyInterned.of(1, 2).hashCode());
    check(ImmutableSillyInterned.of(1, 2).hashCode()).not(ImmutableSillyInterned.of(2, 2).hashCode());
  }

  @Test(expected = IllegalStateException.class)
  public void cannotBuildWrongInvariants() {
    ImmutableSillyValidatedBuiltValue.builder()
        .value(10)
        .negativeOnly(true)
        .build();
  }

  @Test
  public void canBuildCorrectInvariants() {

    ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(true)
        .build();

    ImmutableSillyValidatedBuiltValue.builder()
        .value(10)
        .negativeOnly(false)
        .build();

    ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(false)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void cannotConstructWithWrongInvariants() {
    ImmutableSillyValidatedConstructedValue.of(10, true);
  }

  @Test
  public void canConstructWithCorrectInvariants() {
    ImmutableSillyValidatedConstructedValue.of(-10, true);
    ImmutableSillyValidatedConstructedValue.of(10, false);
    ImmutableSillyValidatedConstructedValue.of(-10, false);
  }
}
