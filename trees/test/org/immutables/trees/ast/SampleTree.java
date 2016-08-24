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
import java.util.List;
import org.immutables.trees.Trees;
import org.immutables.value.Value;

/**
 * Compilation test for Ast Tree generation.
 */
@Trees.Ast
@Trees.Transform
@Trees.Visit
@Value.Enclosing
@Trees.Include(IncludedTree.class)
public class SampleTree {
  interface Expression {}

  interface Term extends Expression {}

  @Value.Immutable
  interface Operator extends Expression {
    @Value.Parameter
    Term left();

    @Value.Parameter
    Term right();

    @Value.Parameter
    Kind operator();

    List<Integer> cardinalities();

    Optional<String> position();

    enum Kind {
      PLUS,
      MINUS
    }
  }

  @Value.Immutable
  interface Identifier extends Term {
    @Value.Parameter
    String name();
  }

  @Value.Immutable(singleton = true, builder = false)
  interface Eof {}

  void use() {

  }
}
