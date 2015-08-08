/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.builder.fixture;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

/**
 * Builders for simple attributes, collection, generic and primitive variations.
 * Builders are public as of style annotation.
 */
class ImprovisedFactories {

  @Builder.Factory
  public static String superstring(int theory, String reality, @Nullable Void evidence) {
    return theory + " != " + reality + ", " + evidence;
  }

  @Builder.Factory
  public static Iterable<Object> concat(List<String> strings, @Value.NaturalOrder SortedSet<Integer> numbers) {
    return Iterables.<Object>concat(strings, numbers);
  }

  @Builder.Factory
  public static int sum(int a, int b) {
    return a + b;
  }

  void use() {

    int sumOf1and2 = new SumBuilder()
        .a(1)
        .b(2)
        .build();

    String superstring = new SuperstringBuilder()
        .theory(0)
        .reality("")
        .evidence(null)
        .build();

    Iterable<Object> concat = new ConcatBuilder()
        .addStrings(superstring)
        .addNumbers(4, 2, 3)
        .build();

    concat.toString();

    String.valueOf(sumOf1and2);
  }
}
