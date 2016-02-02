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
