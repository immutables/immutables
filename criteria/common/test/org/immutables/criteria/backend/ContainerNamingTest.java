/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.backend;

import org.immutables.criteria.Criteria;
import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;

/**
 * Check that container name is correctly derived from class
 */
public class ContainerNamingTest {

  @Test
  public void name() {
    check(ContainerNaming.DEFAULT.name(M1.class)).is("m1");
    check(ContainerNaming.DEFAULT.name(M2.class)).is("m2");
    check(ContainerNaming.DEFAULT.name(M3.class)).is("changed");
    check(ContainerNaming.DEFAULT.name(M4.class)).is("UPPERCASE");
    check(ContainerNaming.DEFAULT.name(MyClass.class)).is("myClass");
  }

  @Criteria
  private static class M1 {}

  @Criteria.Repository
  private static class M2 {}

  @Criteria.Repository(name = "changed")
  private static class M3 {}

  @Criteria.Repository(name = "UPPERCASE")
  private static class M4 {}

  private static class MyClass {}

}
