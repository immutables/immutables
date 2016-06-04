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
package org.immutables.fixture.generics;

import org.immutables.value.Value;

// Compilation test for qualified `validate` call during construction
interface AbstractValidate<T> {

  @Value.Immutable
  static abstract class Concrete1<T> implements AbstractValidate<T> {
    @Value.Parameter
    abstract T reference();

    @Value.Check
    protected Concrete1<T> check() {
      return this;
    }
  }

  @Value.Immutable
  static abstract class Concrete2<T> implements AbstractValidate<T> {
    @Value.Parameter
    abstract T reference();

    @Value.Check
    protected void check() {}
  }
}
