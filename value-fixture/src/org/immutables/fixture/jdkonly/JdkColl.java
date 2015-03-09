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
package org.immutables.fixture.jdkonly;

import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@Value.Style(jdkOnly = true)
public interface JdkColl {
  List<String> str();

  Set<Integer> ints();

  @Value.NaturalOrder
  SortedSet<Integer> ords();

  Set<RetentionPolicy> pols();

  @Value.ReverseOrder
  NavigableSet<Integer> navs();
}
