package org.immutables.fixture;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Field;
import org.junit.Test;

public class PrivateDefaultConstructorTest {

  @Test
  public void testNominal() throws Exception {
    ImmutablePrivateNoargConstructorNominal.class.getDeclaredConstructor();
  }

  @Test(expected = NoSuchMethodException.class)
  public void testOverridePrehash() throws Exception {
    ImmutablePrivateNoargConstructorOverridePrehash.class.getDeclaredMethod("computeHashCode");
  }
  
  @Test
  public void testDoesNotOverridePrehashWhenOff() throws Exception {
    ImmutablePrivateNoargConstructorOptionFalseDoNotAffectPrehash.class.getDeclaredMethod("computeHashCode");
  }

  @Test
  public void testOverridenBySingleton() throws Exception {
    ImmutablePrivateNoargConstructorIsOverriddenBySingleton singleton = ImmutablePrivateNoargConstructorIsOverriddenBySingleton.of();
    Field test = singleton.getClass().getDeclaredField("test");
    test.setAccessible(true);
    assertEquals(singleton.test(), test.get(singleton));
  }
}
