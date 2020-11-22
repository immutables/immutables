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
package org.immutables.fixture.style;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class StyleTest {
  @Test
  public void publicVisibility() {
    check(!Modifier.isPublic(AbstractValueNamingDetected.class.getModifiers()));
    check(Modifier.isPublic(ValueNamingDetected.class.getModifiers()));
  }

  @Test
  public void packageVisibility() {
    check(Modifier.isPublic(LoweredVisibility.class.getModifiers()));
    check(!Modifier.isPublic(ImmutableLoweredVisibility.class.getModifiers()));
  }

  @Test
  public void noBuiltinContainersSupport() throws Exception {
    Class<?> cls = ImmutableNoBuiltinContainers.Builder.class;
    // when containers are supported, the type would be Iterable.class
    check(cls.getMethod("b", List.class)).notNull();
    // when containers are supported there will be convenience String method
    Method c = null;
    try {
      c = cls.getMethod("c", String.class);
    } catch (NoSuchMethodException ex) {
    }
    check(c).isNull();
  }

  @Test
  public void guavaWeakInterner() {
    // we check only interning mechanism generation, not actual "weak" behavior
    check(ImmutableGuavaInterner.of(1)).same(ImmutableGuavaInterner.of(1));
    check(ImmutableGuavaInterner.of(2)).same(ImmutableGuavaInterner.of(2));
  }

  @Test
  public void jdkWeakInterner() {
    // we check only interning mechanism generation, not actual "weak" behavior
    check(ImmutableJdkInterner.of(1)).same(ImmutableJdkInterner.of(1));
    check(ImmutableJdkInterner.of(2)).same(ImmutableJdkInterner.of(2));
  }

  @Test
  public void canBuild() {
    CanBuild.Builder builder = new CanBuild.Builder();
    check(!builder.isBuilderCanBuild());
    builder.a(1);
    check(builder.isBuilderCanBuild());
    builder.b("");
    check(builder.isBuilderCanBuild());
  }

  @Test
  public void nonPublicInitializers() throws Exception {
    Class<?> cls = ImmutableNonPublicInitializers.Builder.class;
    check(cls.getDeclaredMethod("a", int.class).getModifiers() & Modifier.PUBLIC).is(Modifier.PUBLIC);
    check(cls.getDeclaredMethod("b", String.class).getModifiers() & Modifier.PROTECTED).is(Modifier.PROTECTED);
    check(cls.getDeclaredMethod("c", boolean.class).getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)).is(0);
  }

  @Test
  public void transientDerivedFields() throws Exception {
    check(ImmutableTr.class.getDeclaredField("def").getModifiers() & Modifier.TRANSIENT).is(Modifier.TRANSIENT);
    check(ImmutableNonTr.class.getDeclaredField("def").getModifiers() & Modifier.TRANSIENT).is(0);
    check(ImmutableSer.class.getDeclaredField("def").getModifiers() & Modifier.TRANSIENT).is(0);
    check(ImmutableStrSer.class.getDeclaredField("def").getModifiers() & Modifier.TRANSIENT).is(Modifier.TRANSIENT);
  }
  
  @Test
  public void nonFinalInstanceFields()throws Exception {
    Class<ImmutableNonFinalInstanceFields> c = ImmutableNonFinalInstanceFields.class;
    check(c.getDeclaredConstructor().getModifiers() & Modifier.PROTECTED).is(Modifier.PROTECTED);
    check(c.getDeclaredField("a").getModifiers() & Modifier.FINAL).is(0);
    check(c.getDeclaredField("b").getModifiers() & Modifier.FINAL).is(0);
    check(c.getDeclaredField("c").getModifiers() & Modifier.FINAL).is(0);
  }

  @Test
  public void underrides() {
    ImmutableUnderrideObjectMethods b = ImmutableUnderrideObjectMethods.builder().a(42).build();
    check(b).hasToString("%%%");
    check(b.hashCode()).is(-1);
    check(b).is(b);
    check(b).not(ImmutableUnderrideObjectMethods.builder().a(42).build()); // underridden equals by ref

    ImmutableStaticUnderride su = ImmutableStaticUnderride.builder().build();
    check(su).hasToString("!!!");
    check(su.hashCode()).is(-2);
    check(su).is(su);
    check(su).not(ImmutableStaticUnderride.builder().build());

    ImmutableInternUnderride i1 = ImmutableInternUnderride.builder().d(1).build();
    ImmutableInternUnderride i2 = ImmutableInternUnderride.builder().d(1).build();
    check(i1).not().same(i2); // because == in equalTo prevents proper interning
  }
}
