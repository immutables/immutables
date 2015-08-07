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
package org.immutables.serial.fixture;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Set;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Serial.Version(2L)
@Serial.Structural
@Value.Immutable(builder = false)
interface StructFromConstructor {
  @Value.Parameter
  int a();

  @Value.Parameter
  List<Integer> c();

  @Value.Parameter
  Optional<String> os();
  @Value.Parameter
  Multiset<String> bag();

  @Value.Parameter
  Multimap<Integer, String> index();

  @Value.Parameter
  ListMultimap<Integer, String> indexList();

  SetMultimap<Integer, String> indexSet();

  @Value.Parameter
  BiMap<Integer, String> biMap();
}

@Serial.Version(3L)
@Serial.Structural
@Value.Immutable
interface StructFromBuilder {
  int a();

  String s();
  boolean[] array();

  Set<String> c();

  Optional<Boolean> os();
  Multiset<String> bag();
  Multimap<Integer, String> index();
  ListMultimap<Integer, String> indexList();
  SetMultimap<Integer, String> indexSet();
  BiMap<Integer, String> biMap();
}
