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
package org.immutables.fixture;

import com.google.common.collect.ImmutableMap;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.POST;
import org.immutables.common.collect.ImmutableOrdinalSet;
import org.immutables.fixture.ImmutableSampleCopyOfTypes.ByBuilder;
import org.immutables.fixture.ImmutableSampleCopyOfTypes.ByConstructorAndWithers;
import org.junit.Test;
import simple.GetterAnnotation;
import static org.immutables.check.Checkers.*;

public class ValuesTest {

  @Test
  public void generateGetters() throws Exception {
    ImmutableGetters g = ImmutableGetters.builder().ab(0).cd("").ef(true).build();
    check(g.getAb()).is(0);
    check(g.getCd()).is("");
    check(g.isEf());

    check(ImmutableGetterEncloser.builder().build().getOptional()).isNull();

    check(g.getClass().getMethod("getCd").isAnnotationPresent(POST.class));
    check(g.getClass().getMethod("isEf").getAnnotation(GetterAnnotation.class).value()).hasSize(2);
  }

  @Test
  public void ifaceValue() {
    check(ImmutableIfaceValue.builder().getNumber(1).build()).is(ImmutableIfaceValue.of(1));
  }

  @Test
  public void ordering() {
    OrderAttributeValue value = ImmutableOrderAttributeValue.builder()
        .addNatural(3, 2, 4, 1)
        .addReverse("a", "z", "b", "y")
        .putNavigableMap(2, "2")
        .putNavigableMap(1, "1")
        .putReverseMap("a", "a")
        .putReverseMap("b", "b")
        .build();

    check(value.natural()).isOf(1, 2, 3, 4);
    check(value.reverse()).isOf("z", "y", "b", "a");
    check(value.navigableMap().keySet()).isOf(1, 2);
    check(value.reverseMap().keySet()).isOf("b", "a");
  }

  @Test
  public void nonpublic() {
    check(Modifier.isPublic(PrimitiveDefault.class.getModifiers()));
    check(!Modifier.isPublic(ImmutablePrimitiveDefault.class.getModifiers()));
  }

  @Test
  public void primitiveDefault() {
    check(ImmutablePrimitiveDefault.builder().build().def());
    check(ImmutablePrimitiveDefault.builder().def(true).build().def());
    check(!ImmutablePrimitiveDefault.builder().def(false).build().def());
  }

  @Test
  public void sourceOrdering() {
    SourceOrderingEntity v = ImmutableSourceOrderingEntity.builder()
        .a(1)
        .b(2)
        .y(3)
        .z(4)
        .build();

    check(v).hasToString("SourceOrderingEntity{z=4, y=3, b=2, a=1}");
  }

  @Test
  public void requiredAttributesSetChecked() {
    try {
      ImmutableIfaceValue.builder().build();
      check(false);
    } catch (Exception ex) {
      check(ex.getMessage()).contains("getNumber");
    }
  }

  @Test
  public void auxiliary() {
    ImmutableIfaceValue includesAuxiliary = ImmutableIfaceValue.builder().getNumber(1).addAuxiliary("x").build();
    ImmutableIfaceValue excludesAuxiliary = ImmutableIfaceValue.of(1);
    check(includesAuxiliary).is(excludesAuxiliary);
    check(includesAuxiliary.hashCode()).is(excludesAuxiliary.hashCode());
    check(includesAuxiliary).asString().not().contains("auxiliary");
  }

  @Test
  public void builderInheritence() {
    check(ImmutableSillyExtendedBuilder.builder().inheritedField);
  }

  @Test
  public void nullable() {
    ImmutableHasNullable hasNullable = ImmutableHasNullable.builder().build();
    check(hasNullable.def()).isNull();
    check(hasNullable.der()).isNull();
    check(hasNullable.in()).isNull();
    check(ImmutableHasNullable.of(null)).is(hasNullable);
    check(ImmutableHasNullable.of()).is(hasNullable);
    check(ImmutableHasNullable.of().hashCode()).is(hasNullable.hashCode());
    check(hasNullable).hasToString("HasNullable{}");
  }

  @Test
  public void withMethods() {
    ImmutableSillyValidatedBuiltValue value = ImmutableSillyValidatedBuiltValue.builder()
        .value(-10)
        .negativeOnly(true)
        .build();

    try {
      value.withValue(10);
      check(false);
    } catch (Exception ex) {
    }

    check(value.withNegativeOnly(false).withValue(5).value()).is(5);
  }

  @Test
  public void withMethodSetsAndMaps() {
    ImmutableSillyMapHolder holder = ImmutableSillyMapHolder.builder()
        .addZz(RetentionPolicy.CLASS)
        .build();

    check(holder.withZz(Collections.<RetentionPolicy>emptySet()).zz()).isEmpty();
    check(holder.withHolder2(ImmutableMap.of(1, "")).holder2().size()).is(1);
  }

  @Test
  public void lazyValue() {
    SillyLazy v = ImmutableSillyLazy.of(new AtomicInteger());

    check(v.counter().get()).is(0);
    check(v.val1()).is(1);
    check(v.counter().get()).is(1);

    check(v.val2()).is(2);
    check(v.val1()).is(1);
    check(v.counter().get()).is(2);
  }

  @Test
  public void packagePrivateClassGeneration() {
    check(Modifier.isPublic(SillyEmpty.class.getModifiers()));
    check(Modifier.isPublic(ImmutableSillyEmpty.class.getModifiers()));
    check(!Modifier.isPublic(SillyExtendedBuilder.class.getModifiers()));
    check(!Modifier.isPublic(ImmutableSillyExtendedBuilder.class.getModifiers()));
  }

  @Test
  public void ordinalValue() {
    ImmutableSillyOrdinal a = ImmutableSillyOrdinal.of("a");
    ImmutableSillyOrdinal b = ImmutableSillyOrdinal.of("b");
    ImmutableSillyOrdinal c = ImmutableSillyOrdinal.of("c");

    checkAll(a.ordinal(), b.ordinal(), c.ordinal()).isOf(0, 1, 2);
    check(ImmutableSillyOrdinal.of("a")).same(a);
    check(ImmutableSillyOrdinal.of("b")).same(b);

    check(a.domain().get(1)).same(b);
    check(a.domain().get(0)).same(a);
    check(a.domain().length()).is(3);
    check(a.domain()).isOf(a, b, c);
  }

  @Test
  public void ordinalDomain() {
    ImmutableSillyOrdinal.Domain domain = new ImmutableSillyOrdinal.Domain();

    ImmutableSillyOrdinal a = ImmutableSillyOrdinal.of("a");

    ImmutableSillyOrdinal a1 = ImmutableSillyOrdinal.builder()
        .domain(domain)
        .name("a")
        .build();

    ImmutableSillyOrdinal a2 = ImmutableSillyOrdinal.builder()
        .domain(domain)
        .name("a")
        .build();

    check(a.domain()).not(domain);
    check(a.domain()).same(ImmutableSillyOrdinal.Domain.get());
    check(a1.domain()).same(domain);

    check(a).not(a1);
    check(a1).same(a2);
    check(domain.length()).is(1);
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
  public void copyConstructor() {
    ByBuilder wasCopiedByBuilder =
        ImmutableSampleCopyOfTypes.ByBuilder.copyOf(new SampleCopyOfTypes.ByBuilder() {
          @Override
          public int value() {
            return 2;
          }
        });
    check(wasCopiedByBuilder.value()).is(2);

    ByConstructorAndWithers wasCopiedByConstructorAndWithers =
        ImmutableSampleCopyOfTypes.ByConstructorAndWithers.copyOf(new SampleCopyOfTypes.ByConstructorAndWithers() {
          @Override
          public int value() {
            return 1;
          }

          @Override
          public List<String> additional() {
            return Arrays.asList("3");
          }
        });

    check(wasCopiedByConstructorAndWithers.value()).is(1);
    check(wasCopiedByConstructorAndWithers.additional()).isOf("3");

    SampleCopyOfTypes.ByConstructorAndWithers value2 = ImmutableSampleCopyOfTypes.ByConstructorAndWithers.of(2);

    check(ImmutableSampleCopyOfTypes.ByConstructorAndWithers.copyOf(value2))
        .same(ImmutableSampleCopyOfTypes.ByConstructorAndWithers.copyOf(value2));
  }

  @Test
  public void underwriteHashcodeToStringEquals() {
    ImmutableUnderwrite v =
        ImmutableUnderwrite.builder()
            .value(1)
            .build();
    check(v.hashCode()).is(2);
    check(v).hasToString("U");
    check(v.equals("U"));
  }

  @Test
  public void noUnderwriteInheritedHashcodeToStringEquals() {
    ImmutableNoUnderwrite v =
        ImmutableNoUnderwrite.builder()
            .value(1)
            .build();
    check(v.hashCode()).not(2);
    check(v.toString()).not("U");
    check(!v.equals("U"));
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
