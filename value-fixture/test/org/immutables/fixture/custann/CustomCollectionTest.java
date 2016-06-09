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
package org.immutables.fixture.custann;

import static org.immutables.check.Checkers.check;
import java.util.Arrays;
import org.junit.Test;

public class CustomCollectionTest {
  @Test
  public void creates() {
    CustomCollection<String> s = new CustomCollection.Builder<String>()
        .addCint(1, 2, 3)
        .addCol("a", "b", "c")
        .build();

    // actually check is that it was created, empty check is bogus
    // because CustColl always empty
    check(s.cint()).isEmpty();
    check(s.col()).isEmpty();

    CustomCollection<String> s2 = ImmutableCustomCollection.of(Arrays.asList(""));
    check(s2.cint()).isEmpty();
  }
}
