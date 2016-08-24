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
package org.immutables.trees.ast;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;

// Included in SampleTree
public interface IncludedTree {

  @Value.Immutable
  public interface Included1 {}

  @Value.Immutable
  public interface Included2 {
    Optional<Included1> included1();

    Optional<Integer> intOpt();

    Optional<String> stringOpt();

    Set<Integer> intSet();

    Multimap<Integer, Long> multimap();

    Map<String, Object> map();
  }
}
