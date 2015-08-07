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

import static org.immutables.check.Checkers.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.POST;
import nonimmutables.GetterAnnotation;
import org.immutables.fixture.ImmutableSampleCopyOfTypes.ByBuilder;
import org.immutables.fixture.ImmutableSampleCopyOfTypes.ByConstructorAndWithers;
import org.junit.Test;

public class ValuesTest {

  @Test
  public void generateCopyAnnotations() throws Exception {
    ImmutableGetters g = ImmutableGetters.builder().ab(0).cd("").ef(true).build();

    check(g.getClass().getMethod("cd").isAnnotationPresent(POST.class));
    check(g.getClass().getMethod("ef").getAnnotation(GetterAnnotation.class).value()).hasSize(2);
  }

  @Test
  public void internCustomHashCode() {
    ImmutableInternCustomHashCode i1 = ImmutableInternCustomHashCode.builder()
        .a(1)
        .build();

    // customized hash code
    check(i1.hashCode()).is(0);

    // due to overriden equals and interning
    check(i1).same(ImmutableInternCustomHashCode.builder()
        .a(2)
        .build());
  }

  @Test(expected = NullPointerException.class)
  public void orderAndNullCheckForConstructor() {
    ImmutableHostWithPort.of(1, null);
  }

  @Test
  public void builderFrom() {
    SampleValue sv1 = ImmutableSampleValue.builder()
        .addC(1, 2)
        .a(3)
        .oi(1)
        .build();

    SampleValue sv2 = ImmutableSampleValue.builder()
        .a(1)
        .addC(3, 4)
        .os("")
        .build();

    SampleValue svAll = ImmutableSampleValue.builder()
        .from(sv1)
        .from(sv2)
        .build();

    check(svAll.a()).is(1);
    check(svAll.c()).isOf(1, 2, 3, 4);
    check(svAll.oi().orElse(-1)).is(1);
    check(svAll.os()).isOf("");
  }

  @Test
  public void resetCollectionTest() {
    OrderAttributeValue a = ImmutableOrderAttributeValue.builder()
        .addNatural(0)
        .natural(ImmutableList.of(3, 2, 4, 1))
        .addReverse("")
        .reverse(ImmutableList.of("a", "z", "b", "y"))
        .putNavigableMap(1, "2")
        .navigableMap(ImmutableMap.of(2, "2"))
        .reverseMap(ImmutableMap.of("a", "a"))
        .build();

    OrderAttributeValue b = ImmutableOrderAttributeValue.builder()
        .addNatural(3, 2, 4, 1)
        .addReverse("a", "z", "b", "y")
        .putNavigableMap(2, "2")
        .putReverseMap("a", "a")
        .build();

    check(a).is(b);
  }

  @Test
  public void extendingBuilder() {
    ExtendingInnerBuilderValue.Builder builder = new ExtendingInnerBuilderValue.Builder();
    ExtendingInnerBuilderValue value = builder.addList("").build();
    check(value.attribute()).is(1);
    check(value.list()).isOf("");
  }

  @Test
  public void defaultAsDefault() {
    DefaultAsDefault d = ImmutableDefaultAsDefault.builder()
        .b(1)
        .build();

    check(d.a()).is(1);
    check(d.b()).is(1);
  }

  @Test
  public void extendsBuilderIfaceValue() {
    ExtendedBuilderInterface ifc = ExtendedBuilderInterface.builder()
        .a(1)
        .b(2)
        .build();

    check(ifc.a()).is(1);
    check(ifc.b()).is(2);
  }

  @Test
  public void ifaceValue() {
    check(ImmutableIfaceValue.builder().number(1).build()).is(ImmutableIfaceValue.of(1));
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
      check(ex.getMessage()).contains("number");
    }
  }

  @Test
  public void auxiliary() {
    ImmutableIfaceValue includesAuxiliary = ImmutableIfaceValue.builder().number(1).addAuxiliary("x").build();
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
    check(hasNullable).hasToString("HasNullable{in=null, def=null, der=null}");
  }

  @Test
  public void java8TypeAnnotation() {
    // FIXME Type Annotations
    // HasTypeAnnotation hasTypeAnnotation = ImmutableHasTypeAnnotation.builder().build();
    // check(hasTypeAnnotation.str()).isNull();
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
    check(v.toString()).not("N");
    check(!v.equals("N"));
  }

  @Test
  public void recalculateDerivedOnCopy() {
    ImmutableWitherDerived value = ImmutableWitherDerived.builder()
        .set(1)
        .build();

    check(value.derived()).is(value.set() + 1);
    value = value.withSet(2);
    check(value.derived()).is(value.set() + 1);
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
