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

import org.immutables.value.Value;

@Value.Enclosing
public enum ConstructorParameterInheritanceLevels {
  ;
  interface A {
    @Value.Parameter
    int a();
  }

  @Value.Immutable
  interface B extends A {
    @Value.Parameter
    int b();

    @Override
    @Value.Parameter
    int a();
  }

  interface C<T, V> {
    @Value.Parameter
    T a();

    @Value.Parameter
    V b();
  }

  @Value.Immutable
  interface D extends C<A, B> {}
}
