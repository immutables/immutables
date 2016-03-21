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

import java.util.Collections;
import java.util.List;
import org.immutables.value.Value;

//Compilation tests for singletons/builders
public interface Singletons {}

@Value.Immutable(singleton = true)
interface Sing1 {
  List<Integer> list();

  default void use() {
    ImmutableSing1.builder();
    ImmutableSing1.of()
        .withList(Collections.emptyList());
  }
}

@Value.Immutable(builder = false)
interface Sing2 {
  @Value.Default
  default int a() {
    return 1;
  }

  default void use() {
    ImmutableSing2.of().withA(1);
  }
}

@Value.Immutable(builder = false)
interface Sing3 {
  default void use() {
    ImmutableSing3.of();
  }
}

@Value.Immutable(singleton = true)
interface Sing4 {

  default void use() {
    ImmutableSing4.builder();
    ImmutableSing4.of();
  }
}

@Value.Immutable(builder = false, singleton = true)
interface Sing5 {
  @Value.Parameter
  @Value.Default
  default int a() {
    return 1;
  }

  default void use() {
    ImmutableSing5.of();
    ImmutableSing5.of(1);
  }
}

@Value.Immutable
interface Sing6 {
  default void use() {
    ImmutableSing6.builder();
  }
}

@Value.Immutable(builder = false)
@Value.Style(attributelessSingleton = true)
interface Sing7 {
  default void use() {
    ImmutableSing7.of();
  }
}
