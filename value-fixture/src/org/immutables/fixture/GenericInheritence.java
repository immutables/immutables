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
package org.immutables.fixture;

import java.io.Serializable;
import org.immutables.value.Value;

public interface GenericInheritence {

  public static interface Gen<A extends Object & Comparable<A>, B extends Serializable> {
    A a();

    B b();
  }

  @Value.Immutable
  public static interface Sub1 extends Gen<String, Integer> {}

  @Value.Immutable
  public static interface Sub2 extends Gen<Long, Double> {}
}
