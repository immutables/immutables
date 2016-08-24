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

import org.immutables.trees.Trees.Visit;
import com.google.common.base.Optional;
import java.util.List;
import org.immutables.trees.Trees.Ast;
import org.immutables.trees.Trees.Transform;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.ImplementationVisibility;

// some of the real world compilation test
@Style(visibility = ImplementationVisibility.PUBLIC, overshadowImplementation = true, strictBuilder = true)
@Ast
@Visit
@Transform
@Enclosing
public interface Grammar {

  @Immutable(builder = false)
  interface Literal {
    @Parameter
    String value();
  }

  @Immutable(builder = false)
  interface Identifier {
    @Parameter
    String value();
  }

  interface Tagged {
    Optional<Identifier> tag();
  }

  interface Named {
    Identifier name();
  }

  interface Part extends Tagged {

    Cardinality cardinality();
  }

  @Immutable
  interface CharRange extends Part {
    List<CharRangeElement> elements();
  }

  @Immutable
  interface CharRangeElement {
    boolean negated();

    Char from();

    Char to();
  }

  interface Char {}

  @Immutable(builder = false)
  interface CharLiteral extends Char {
    @Parameter
    String value();
  }

  @Immutable(builder = false)
  interface CharEscape extends Char {
    @Parameter
    String value();
  }

  @Immutable
  interface LiteralPart extends Part {
    Literal literal();
  }

  @Immutable
  interface ReferencePart extends Part {
    Identifier reference();
  }

  @Immutable
  interface FirstOfPart extends Part, Choice {}

  interface Choice {
    List<Alternative> alternatives();
  }

  interface Production {
    List<Part> parts();
  }

  @Immutable
  interface Seq extends Production {}

  @Immutable
  interface Alternative extends Production, Tagged {}

  @Immutable
  interface SyntaxProduction extends Named, Choice {}

  @Immutable
  interface LexProduction extends Named, Production {}

  enum Cardinality {
    ONE, ZERO_OR_ONE, ZERO_OR_MORE, ONE_OR_MORE;
  }
}
