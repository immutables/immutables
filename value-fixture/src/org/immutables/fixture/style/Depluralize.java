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
package org.immutables.fixture.style;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;
import org.immutables.value.Value.Style;

/**
 * Compilation test to demonstrate generated builder methods
 * according to logic and examples from {@link Style#depluralize()} javadoc.
 */
@Value.Immutable
@Value.Style(depluralize = {"foot:feet", "person:people", "goods"})
public interface Depluralize {

  List<String> feet();

  Set<String> boats();

  Map<String, String> people();

  Multimap<String, String> peopleRepublics();

  List<Integer> feetPeople();

  Multiset<Boolean> goods();

  default void use() {
    ImmutableDepluralize.builder()
        .addBoat("") // automatically trims s
        .addFoot("") // applies "foot:feet"
        .addGoods(true) // used literally due to exception "goods"
        .addFeetPerson(1) // last segment converted using "person:people"
        .putPerson("", "") // converted using "person:people"
        .putPeopleRepublic("", "") // last segment s auto-trimmed
        .build();
  }
}
