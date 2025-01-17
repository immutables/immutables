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
package org.immutables.builder.fixture;

import java.util.Set;
import com.google.common.collect.Iterables;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

class GenericsImprovisedFactories2 {
  @Builder.Factory
  public static <K extends Object & Runnable> Iterable<K> genericConcat(
      List<K> strings,
      Set<K> numbers) {
    return Iterables.<K>concat(strings, numbers);
  }

  void use() {

    class X implements Runnable {
      @Override
      public void run() {}
    }
    Iterable<X> concat = new GenericConcatBuilder<X>()
        .addStrings(new X())
        .addNumbers(new X(), new X(), new X())
        .build();

    concat.toString();
  }
}

@Value.Style(newBuilder = "newBuilder")
class GenericsImprovisedFactories {

  @Builder.Factory
  @SuppressWarnings("all")
  public static <T, @Nullable V extends RuntimeException> String genericSuperstring(int theory, T reality, @Nullable V evidence)
      throws V {
    if (evidence != null) {
      throw evidence;
    }
    return theory + " != " + reality;
  }

  void use() {

    String superstring = GenericSuperstringBuilder.<String, IllegalArgumentException>newBuilder()
        .theory(0)
        .reality("")
        .evidence(null)
        .build();

    superstring.toString();
  }
}
