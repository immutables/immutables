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
package org.immutables.func.fixture;

import org.immutables.value.Value;
import org.immutables.func.Functional;

@Value.Immutable
public abstract class Compute {
  @Functional
  public abstract String getX();

  @Functional.BindParameters
  public String computeZ(String y) {
    return getX() + y;
  }

  @Functional.BindParameters
  public int computeH(int a, int b, float c) {
    return (int) (a + b + c);
  }
}
