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
package org.immutables.fixture.strict;

import com.google.common.base.Optional;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.immutables.value.Value;

@Value.Immutable
interface Aar {
  boolean bl();

  Integer integer();
}

@Value.Immutable
interface Bar {
  List<Integer> nums();

  Map<String, Integer> map();

  ImmutableMap<String, Integer> immutableMap();

  SortedMap<String, Integer> sortedMap();

  ImmutableSortedMap<String, Integer> immutableSortedMap();

  Set<String> set();

  ImmutableSet<String> immutableSet();

  SortedSet<String> sortedSet();

  ImmutableSortedSet<String> immutableSortedSet();

  Optional<Integer> opt();

  EnumMap<BoundType, Integer> enumMap();

  EnumSet<BoundType> enumSet();
}
