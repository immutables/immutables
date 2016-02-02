/*
   Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.check;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

/** The match checks test. */
public class CheckersTest {
  @Test
  public void showcase() throws Exception {
    check(Arrays.asList("xx", "yy", "zz")).hasAll("yy", "xx");
    check(new String[] {
        "xx", "yy", "zz"
    }).isOf("xx", "yy", "zz");
    check(Optional.of("")).isOf("");
    check(Optional.absent()).isAbsent();
    check("abc").matches("\\w{1,3}");
    check(new ArrayList<>()).isA(List.class);
    check(true);
    check((Void) null).isNull();
  }
}
