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
package org.immutables.generator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SourceTypeTest {
  @Test
  public void extract() {
    Entry<String, List<String>> sourceTypes = SourceTypes.extract("Map<String<X>,  Map<Int, B>>");

    check(sourceTypes.getKey()).is("Map");
    check(sourceTypes.getValue()).isOf("String<X>", "Map<Int, B>");
  }

  public void stringify() {
    Entry<String, List<String>> entry =
        Maps.<String, List<String>>immutableEntry("Map",
            ImmutableList.of("String<X>", "Map<Int, B>"));

    check(SourceTypes.stringify(entry)).is("Map<String<X>,  Map<Int, B>>");
  }
}
