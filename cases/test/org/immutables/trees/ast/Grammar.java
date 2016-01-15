package org.immutables.trees.ast;

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
