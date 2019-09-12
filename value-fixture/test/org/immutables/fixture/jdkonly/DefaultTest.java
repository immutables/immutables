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
package org.immutables.fixture.jdkonly;

import com.google.common.collect.ImmutableSortedSet;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class DefaultTest {
  @Test
  public void defArray() {
    ImmutableDefaultArray a1 = ImmutableDefaultArray.builder().build();
    int[] array = a1.prop();
    int[] nextArray = a1.prop();
    check(array != nextArray);
    check(a1.ints() instanceof ImmutableSortedSet);
  }
}
