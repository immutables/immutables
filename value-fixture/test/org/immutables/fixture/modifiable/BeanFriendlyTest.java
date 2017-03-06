package org.immutables.fixture.modifiable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.beans.BeanInfo;
import java.beans.Introspector;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class BeanFriendlyTest {

  @Test
  public void modifiableAsJavaBean() throws Exception {
    ImmutableSet<String> rwProperties = ImmutableSet.of("primary", "id", "description", "names", "options");

    BeanInfo beanInfo = Introspector.getBeanInfo(ModifiableBeanFriendly.class);

    for (java.beans.PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      if (rwProperties.contains(pd.getName())) {
        check(pd.getReadMethod()).notNull();
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
