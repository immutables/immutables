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
package org.immutables.fixture.nullable;

import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;

import static org.immutables.check.Checkers.check;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NullableAttributesTest {

  @Test
  public void defaultValues() {
    ImmutableNullableAttributes a1 = ImmutableNullableAttributes.builder()
        .build();

    check(a1.array()).isNull();
    check(a1.integer()).isNull();
    check(a1.defArray()).isOf(Double.NaN);
    check(a1.list()).isNull();
    check(a1.set()).notNull();
    check(a1.set()).isEmpty();
    check(a1.map()).isNull();
  }

  @Test
  public void explicitNulls() {
    ImmutableNullableAttributes a1 = ImmutableNullableAttributes.builder()
        .set(null)
        .defArray((Double[]) null)
        .build();

    check(a1.defArray()).isNull();
    check(a1.set()).isNull();
  }

  @Test
  public void simpleNullableDefaultExplicit() {
    ImmutableNullableDefault o1 = ImmutableNullableDefault.builder().build();
    ImmutableNullableDefault o2 = ImmutableNullableDefault.builder()
        .intr(null)
        .str(null)
        .build();

    check(o1.intr()).is(-1);
    check(o1.str()).is("def");

    check(o2.intr()).isNull();
    check(o2.str()).isNull();
  }

  @Test
  public void nonDefaultValues() {
    ImmutableNullableAttributes a1 = ImmutableNullableAttributes.builder()
        .addSet(1)
        .defArray(1.0, 2.0)
        .addList("a")
        .addAllList(Arrays.asList("b", "c"))
        .putMap("key", new Object())
        .build();

    check(a1.set()).isOf(1);
    check(a1.defArray()).isOf(1.0, 2.0);
    check(a1.list()).isOf("a", "b", "c");
    check(a1.map().keySet()).isOf("key");
  }

  @Test
  public void fromNullCollection() {
    ImmutableFromNullCollection nullCollection = ImmutableFromNullCollection.builder()
        .freq(null)
        .items(null)
        .build();

    ImmutableFromNullCollection fromNullCollection = ImmutableFromNullCollection.builder()
        .from(nullCollection)
        .build();

    check(fromNullCollection.getItems()).isNull();
    check(fromNullCollection.getFreq()).isNull();
  }

  @Test
  public void compactConstruction() {
    ImmutableNullableCompact c1 = ImmutableNullableCompact.builder()
        .build();

    check(ImmutableNullableCompact.of(null, null)).is(c1);
  }

  @SuppressWarnings("CheckReturnValue")
  @Test
  public void nonnullDefaultBlowupOnNull() {
    assertThrows(NullPointerException.class, () ->
    ImmutableNonnullConstruction.builder()
        .arr()
        .build());
  }

  @Test
  public void nonnullDefault() {
    check(ImmutableNonnullConstruction.builder()
        .arr()
        .addAx("a")
        .addAx("b", "c")
        .build()
        .ax())
            .isOf("a", "b", "c");
  }

  @Test
  public void skipNullElements() {
    ImmutableNullablyElements elements = ImmutableNullablyElements.builder()
        .addSk((String) null)
        .addSk("a")
        .addSk((String) null)
        .addSk(null, "b", null)
        .addAllSk(Arrays.asList(null, "c", "d"))
        .build();

    check(elements.sk()).isOf("a", "b", "c", "d");

    elements = elements.withSk("", null, "");

    check(elements.sk()).isOf("", "");
  }

  @Test
  public void skipNullValues() {
    ImmutableNullablyElements elements = ImmutableNullablyElements.builder()
        .putSm("a", null)
        .putSm("b", 1)
        .putAllSm(Collections.singletonMap("c", null))
        .build();

    check(elements.sm()).hasToString("{b=1}");
  }

  @Test
  public void allowNulls() {
    ImmutableNullablyElements elements = ImmutableNullablyElements.builder()
        .addAl((Void) null)
        .addAl(null, null, null)
        .addAllAl(Arrays.asList((Void) null))
        .putBl("a", null)
        .putBl(null, 1) // this is questionable, but let it be
        .build();

    check(elements.al()).isOf(null, null, null, null, null);
    check(elements.bl()).hasToString("{a=null, null=1}");
  }

  @Test
  public void banNulls() {
    ImmutableNullablyElements.Builder b = ImmutableNullablyElements.builder();
    try {
      b.addRg((String) null);
      check(false);
    } catch (NullPointerException ex) {
    }

    try {
      b.addRg("a", null);
      check(false);
    } catch (NullPointerException ex) {
    }

    try {
      b.addAllRg(Arrays.asList("b", null));
      check(false);
    } catch (NullPointerException ex) {
    }
  }

  @Test
  public void skipNullElements1() {
    ImmutableNullAnnElements elements = ImmutableNullAnnElements.builder()
        .addSk((String) null)
        .addSk("a")
        .addSk((String) null)
        .addSk(null, "b", null)
        .addAllSk(Arrays.asList(null, "c", "d"))
        .build();

    check(elements.sk()).isOf("a", "b", "c", "d");

    elements = elements.withSk("", null, "");

    check(elements.sk()).isOf("", "");
  }

  @Test
  public void skipNullValues2() {
    ImmutableNullAnnElements elements = ImmutableNullAnnElements.builder()
        .putSm("a", null)
        .putSm("b", 1)
        .putAllSm(Collections.singletonMap("c", null))
        .build();

    check(elements.sm()).hasToString("{b=1}");
  }

  @Test
  public void allowNulls2() {
    ImmutableNullAnnElements elements = ImmutableNullAnnElements.builder()
        .addAl((Void) null)
        .addAl(null, null, null)
        .addAllAl(Arrays.asList((Void) null))
        .putBl("a", null)
        .putBl(null, 1) // this is questionable, but let it be
        .build();

    check(elements.al()).isOf(null, null, null, null, null);
    check(elements.bl()).hasToString("{a=null, null=1}");
  }

  @Test
  public void banNulls2() {
    ImmutableNullAnnElements.Builder b = ImmutableNullAnnElements.builder();
    try {
      b.addRg((String) null);
      check(false);
    } catch (NullPointerException ex) {
    }

    try {
      b.addRg("a", null);
      check(false);
    } catch (NullPointerException ex) {
    }

    try {
      b.addAllRg(Arrays.asList("b", null));
      check(false);
    } catch (NullPointerException ex) {
    }
  }

  @Test
  public void skipNullValues3() {
    ImmutableNullablyElements elements = ImmutableNullablyElements.builder()
        .addAllRi(Arrays.asList(1, null, 2, null))
        .build();

    check(elements.ri()).isOf(1, 2);
  }

  @Test
  public void allowNulls3() {
    ImmutableNullablyElements elements = ImmutableNullablyElements.builder()
        .addAllRj(Arrays.asList(1, null, 2, null))
        .build();

    check(elements.rj()).isOf(1, null, 2, null);
  }

  @Test
  public void banNulls3() {
    try {
      ImmutableNullablyElements elements = ImmutableNullablyElements.builder()
          .addAllRk(Arrays.asList(1, null, 2, null))
          .build();
    } catch (NullPointerException ex) {
      // make sure NPE isn't due to unboxed loop, static code analyzers don't like that
      check(ex.getStackTrace()[0].getMethodName().equals("requireNonNull"));
    }
  }

  @Test
  public void typeUseNullable() {
    ImmutableNullableTypeUseJdtAccepted r = ImmutableNullableTypeUseJdtAccepted.builder()
        .i1(null)
        .l2(null)
        .build();

    check(r.i1()).isNull();
    check(r.l2()).isNull();
  }

  @Test
  public void nullableFromSupertypes() {
    ImmutableSubtype s = ImmutableSubtype.builder()
        // default a, b
        .from(new FromSupertypeNullable.Subtype() {
          @Override
          public String a() {
            return "a";
          }

          @Override
          public String b() {
            return "b";
          }
        })
        // would not trip on NPE from supertype which declares it nullable
        .from(new FromSupertypeNullable.SuperA() {
          @Override
          @Nullable
          public String a() {
            return null;
          }
        })
        // would not trip on NPE from supertype which declares it nullable
        .from(new FromSupertypeNullable.SuperB() {
          @Override
          @Nullable
          public String b() {
            return null;
          }
        })
        .build();

    check(s.a()).is("a");
    check(s.b()).is("b");
  }
}
