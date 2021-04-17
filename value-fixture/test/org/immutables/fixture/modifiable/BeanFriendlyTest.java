/*
   Copyright 2017 Immutables Authors and Contributors

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
package org.immutables.fixture.modifiable;

import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashSet;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class BeanFriendlyTest {

  @Test
  public void modifiableAsJavaBean() throws Exception {
    ImmutableSet<String> rwProperties =
        ImmutableSet.of("primary", "id", "description", "names", "options", "extra");

    FluentIterable<PropertyDescriptor> descriptors =
        FluentIterable.of(
            Introspector.getBeanInfo(ModifiableBeanFriendly.class)
                .getPropertyDescriptors());

    check(descriptors.transform(FeatureDescriptor::getName).toSet().containsAll(rwProperties));

    for (PropertyDescriptor pd : descriptors) {
      String name = pd.getName();
      try {
        if (rwProperties.contains(pd.getName())) {
          Field f = ModifiableBeanFriendly.class.getDeclaredField(name);
          Method r = pd.getReadMethod();
          Method w = pd.getWriteMethod();
          check(r).notNull();
          check(r.getModifiers() & Modifier.FINAL).is(0);
          check(f.getModifiers() & Modifier.FINAL).is(0);
          check(f.getType() == r.getReturnType());
          check(w).notNull();
          check(w.getModifiers() & Modifier.FINAL).is(0);
        }
      } catch (Exception | AssertionError a) {
        throw new AssertionError("property '" + name + "': " + a.getMessage(), a);
      }
    }

    ModifiableBeanFriendly bean = new ModifiableBeanFriendly();
    bean.setPrimary(true);
    bean.setDescription("description");
    bean.setId(1000);
    bean.setNames(new HashSet<>());
    bean.addNames("name");
    bean.addNames("name2");
    bean.putOptions("foo", "bar");

    // This bean can become immutable.
    BeanFriendly immutableBean = bean.toImmutable();
    check(immutableBean.isPrimary());
    check(immutableBean.getDescription()).is("description");
    check(immutableBean.getId()).is(1000);
    check(immutableBean.getNames()).hasAll("name", "name2");
    check(immutableBean.getOptions()).is(ImmutableMap.of("foo", "bar"));

    // from works as with Immutable
    BeanFriendly mutableFromImmutable1 = new ModifiableBeanFriendly().from(immutableBean);
    BeanFriendly.Mod mutableFromImmutable2 = new ModifiableMod().from(ImmutableMod.builder().build());
  }
}
