/*
   Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.generator.processor;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import java.util.List;
import org.immutables.generator.StringLiterals;
import org.immutables.trees.Trees.Ast;
import org.immutables.trees.Trees.Transform;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

/**
 * Abstract syntax trees.
 */
@Enclosing
@Transform
@Ast
public class Trees {
  private Trees() {}

  @Immutable(builder = false)
  public static abstract class Identifier {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return "`" + value() + "`";
    }
  }

  @Immutable(builder = false)
  public static abstract class TypeIdentifier {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return "`" + value() + "`";
    }
  }

  public interface TypeReference {}

  @Immutable
  public static abstract class TypeDeclaration implements TypeReference {
    public abstract TypeIdentifier type();

    @Default
    public Kind kind() {
      return Kind.SCALAR;
    }

    public enum Kind {
      SCALAR,
      ITERABLE
    }
  }

  @Immutable
  public abstract static class ResolvedType implements Trees.TypeReference, Trees.Synthetic {
    @Value.Parameter
    public abstract Object type();

    @Override
    public String toString() {
      return type().toString();
    }
  }

  @Immutable
  public abstract static class BoundAccessExpression implements Trees.AccessExpression, Trees.Synthetic {
    public abstract List<Object> accessor();

    @Override
    public String toString() {
      return accessor().toString();
    }
  }

  @Immutable
  public interface InvokableDeclaration extends Named {
    List<Parameter> parameters();
  }

  @Immutable
  public interface ValueDeclaration extends Named {
    Optional<TypeReference> type();

    Optional<TypeReference> containedType();

    ValueDeclaration withType(TypeReference reference);

    ValueDeclaration withContainedType(TypeReference reference);
  }

  public interface Typed {
    TypeReference type();
  }

  @Immutable
  public interface Parameter extends Named, Typed {}

  public interface Named {
    Identifier name();
  }

  public interface InvokableStatement {
    InvokableDeclaration declaration();
  }

  public interface Block extends TemplatePart {
    List<TemplatePart> parts();
  }

  @Immutable
  public interface SimpleBlock extends Block {}

  @Immutable(singleton = true, builder = false)
  public interface Comment extends UnitPart, TemplatePart {}

  @Immutable
  public interface ConditionalBlock extends Conditional, Block, Synthetic {}

  @Immutable
  public interface IfStatement extends TemplatePart, Synthetic {
    ConditionalBlock then();

    List<ConditionalBlock> otherwiseIf();

    Optional<Block> otherwise();
  }

  @Immutable
  public abstract static class ForStatement implements Block, Synthetic {
    @Default
    public boolean useForAccess() {
      return true;
    }

    @Default
    public boolean useDelimit() {
      return true;
    }

    public abstract List<GeneratorDeclaration> declaration();
  }

  @Immutable(builder = false)
  public interface ForIterationAccessExpression extends AccessExpression {
    @Value.Parameter
    AccessExpression access();
  }

  @Immutable
  public interface LetStatement extends Block, InvokableStatement, Synthetic {}

  @Immutable
  public interface InvokeStatement extends Block, Synthetic {
    Expression access();

    List<Expression> params();
  }

  public interface DirectiveStart extends Directive {}

  public interface DirectiveEnd extends Directive {}

  @Immutable(singleton = true, builder = false)
  public interface LetEnd extends DirectiveEnd {}

  @Immutable(singleton = true, builder = false)
  public interface ForEnd extends DirectiveEnd {}

  @Immutable(singleton = true, builder = false)
  public interface IfEnd extends DirectiveEnd {}

  @Immutable(builder = false)
  public interface InvokeEnd extends DirectiveEnd {
    @Value.Parameter
    AccessExpression access();
  }

  @Immutable(builder = false)
  public interface InvokeString extends DirectiveStart {
    @Value.Parameter
    StringLiteral literal();
  }

  public interface InvokeDeclaration {
    AccessExpression access();

    Optional<ApplyExpression> invoke();
  }

  @Immutable
  public interface Invoke extends InvokeDeclaration, DirectiveStart {}

  public interface Directive extends TemplatePart {}

  @Immutable
  public interface Let extends DirectiveStart, InvokableStatement {}

  public interface UnitPart {}

  @Immutable
  public interface Unit {
    List<UnitPart> parts();
  }

  public interface TemplatePart {}

  /**
   * Non parser generated expressions or statements, produced by AST transformations, typing etc.
   */
  public interface Synthetic {}

  @Immutable
  public static abstract class Template implements Directive, Block, UnitPart, InvokableStatement {
    @Default
    public boolean isPublic() {
      return false;
    }
  }

  public interface Expression {}

  public interface AccessExpression extends Expression {
    List<Identifier> path();
  }

  @Immutable
  public interface SimpleAccessExpression extends AccessExpression {}

  @Immutable
  public interface ApplyExpression extends Expression {
    List<Expression> params();
  }

  public interface GeneratorDeclaration {}

  public interface GeneratorValueDeclaration extends GeneratorDeclaration {
    ValueDeclaration declaration();

    Expression from();
  }

  @Immutable
  public interface AssignGenerator extends GeneratorValueDeclaration {}

  @Immutable
  public interface IterationGenerator extends GeneratorValueDeclaration {
    Optional<Expression> condition();
  }

  @Immutable
  public interface TransformGenerator extends GeneratorValueDeclaration {
    Expression transform();

    ValueDeclaration varDeclaration();

    Optional<Expression> condition();
  }

  @Immutable
  public interface For extends DirectiveStart {
    List<GeneratorDeclaration> declaration();
  }

  @Immutable
  public interface If extends Conditional, DirectiveStart {}

  public interface Otherwise extends DirectiveStart {}

  @Immutable
  public interface ElseIf extends Otherwise, Conditional {}

  public interface Conditional {
    Expression condition();
  }

  @Immutable(singleton = true, builder = false)
  public interface Else extends Otherwise {}

  @Immutable(singleton = true, builder = false)
  public interface TemplateEnd extends DirectiveEnd, Synthetic {}

  public interface TextPart {}

  @Immutable(builder = false)
  public static abstract class StringLiteral implements Expression {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return StringLiterals.toLiteral(value());
    }
  }

  @Immutable(singleton = true, builder = false)
  public static abstract class Newline implements TextPart {
    @Override
    public String toString() {
      return StringLiterals.toLiteral("\n");
    }
  }

  @Immutable(builder = false)
  public static abstract class TextFragment implements TextPart {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return StringLiterals.toLiteral(value());
    }

    public boolean isWhitespace() {
      return CharMatcher.WHITESPACE.matchesAllOf(value());
    }
  }

  @Immutable
  public interface TextBlock extends TemplatePart {
    List<TextPart> parts();
  }

  @Immutable
  public static abstract class TextLine implements TemplatePart, Synthetic {
    public abstract TextFragment fragment();

    public boolean isBlank() {
      return fragment().isWhitespace();
    }

    public boolean isEmpty() {
      return fragment().value().isEmpty();
    }

    @Default
    public boolean newline() {
      return false;
    }

    @Override
    public String toString() {
      return StringLiterals.toLiteral(
          fragment().value()
              + (newline() ? "\n" : ""));
    }
  }
}
