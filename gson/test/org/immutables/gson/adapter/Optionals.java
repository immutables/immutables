/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.gson.adapter;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

@Gson.TypeAdapters
@Value.Enclosing
public interface Optionals {

  @Gson.ExpectedSubtypes(D.class)
  interface A {}

  @Value.Immutable(builder = false)
  interface B extends A {
    @Value.Parameter
    int v1();
  }

  @Value.Immutable(builder = false)
  interface C extends A {
    @Value.Parameter
    String v2();
  }

  @Value.Immutable(builder = false)
  interface D extends A {
    @Value.Parameter
    boolean v1();
  }

  @Value.Immutable
  interface Host {

    OptionalInt optInt();

    OptionalLong optLong();

    OptionalDouble optDouble();

    Optional<Float> optWrappedFloat();

    Optional<Integer> optWrappedInteger();

    Optional<Long> optWrappedLong();

    Optional<Double> optWrappedDouble();

    Optional<A> aFrom();

    @Gson.ExpectedSubtypes({B.class, C.class, D.class})
    Optional<A> aPoly();

    @Gson.ExpectedSubtypes({})
    Map<String, A> mappedOptional();

    @Gson.ExpectedSubtypes({})
    List<A> listOptional();

  }
}
