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
package org.immutables.fixture.ast;

import java.util.List;
import org.immutables.value.Value;

public interface InstantiationGenerics {

  interface TreeElement<T> {}

  interface Node<T> extends TreeElement<T> {
    List<TreeElement<T>> elements();
  }

  interface Leaf<T> extends TreeElement<T> {
    @Value.Parameter
    T value();
  }


  @Value.Immutable
  interface StringNode extends Node<String> {}

  @Value.Immutable
  interface StringLeaf extends Leaf<String> {}

  default void use() {
    TreeElement<String> tree =
        ImmutableStringNode.builder()
            .addElements(ImmutableStringLeaf.of("A"))
            .addElements(ImmutableStringLeaf.of("B"))
            .addElements(ImmutableStringNode.builder()
                .addElements(ImmutableStringLeaf.of("C"))
                .addElements(ImmutableStringLeaf.of("D"))
                .build())
            .build();

    tree.toString();
  }
}
