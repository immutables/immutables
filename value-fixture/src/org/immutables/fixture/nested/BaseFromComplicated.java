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
package org.immutables.fixture.nested;

import org.immutables.value.Value;
import java.util.List;

public interface BaseFromComplicated {

  interface A {
    int a();

    List<String> b();
  }

  interface AA {
    int a();
  }

  interface B extends AA {
    List<String> b();
  }

  @Value.Immutable
  interface AB extends A, B {
    @Override
    int a();

    int c();

    @Override
    List<String> b();
  }

  @Value.Modifiable
  @Value.Immutable
  interface AAA extends AA, A {
    @Override
    int a();

    @Override
    List<String> b();
  }
}
