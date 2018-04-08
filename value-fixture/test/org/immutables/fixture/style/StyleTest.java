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
import org.junit.Test;
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
}
