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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;
import static org.immutables.matcher.ModifierMatcher.notFinalModifier;

public class BeanFriendlyTest {
 
  @Test
  public void modifiableAsJavaBean() throws Exception {
    ImmutableSet<String> rwProperties =
        ImmutableSet.of("primary", "id", "description", "names", "options", "extra");

    FluentIterable<PropertyDescriptor> propertyDescriptors =
        FluentIterable.of(
            Introspector.getBeanInfo(ModifiableBeanFriendly.class)
                .getPropertyDescriptors());

    check(propertyDescriptors.transform(p -> p.getName()).toSet().containsAll(rwProperties));

    for (PropertyDescriptor pd : propertyDescriptors) {
      check(pd.getReadMethod()).notNull();
      if (rwProperties.contains(pd.getName())) {
        check(pd.getWriteMethod()).notNull();
      }
    }

    ModifiableBeanFriendly bean = new ModifiableBeanFriendly();
    bean.setPrimary(true);
    bean.setDescription("description");
    bean.setId(1000);
    bean.setNames(ImmutableList.of("name"));
    bean.addNames("name2");
    bean.putOptions("foo", "bar");

    // This bean can become immutable.
    BeanFriendly immutableBean = bean.toImmutable();
    check(immutableBean.isPrimary());
    check(immutableBean.getDescription()).is("description");
    check(immutableBean.getId()).is(1000);
    check(immutableBean.getNames()).isOf("name", "name2");
    check(immutableBean.getOptions()).is(ImmutableMap.of("foo", "bar"));
  }
}
