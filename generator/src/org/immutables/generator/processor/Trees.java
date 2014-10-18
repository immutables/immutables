/*
    Copyright 2014 Ievgen Lukash

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
import org.immutables.value.Parboil;
import org.immutables.value.Value;

/**
 * Abstract syntax trees.
 */
@Value.Nested
@Value.Transformer
@Parboil.Ast
public class Trees {

  @Value.Immutable(builder = false)
  public static abstract class Identifier {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return "`" + value() + "`";
    }
  }

  @Value.Immutable(builder = false)
  public static abstract class TypeIdentifier {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return "`" + value() + "`";
    }
  }

  public interface TypeReference {}

  @Value.Immutable
  public static abstract class TypeDeclaration implements TypeReference {
    public abstract TypeIdentifier type();

    @Value.Default
    public Kind kind() {
      return Kind.SCALAR;
    }

    public enum Kind {
      SCALAR,
      ITERABLE
    }
  }

  @Value.Immutable
  public abstract static class ResolvedType implements Trees.TypeReference, Trees.Synthetic {
    @Value.Parameter
    public abstract Object type();

    @Override
    public String toString() {
      return type().toString();
    }
  }

  @Value.Immutable
  public abstract static class BoundAccessExpression implements Trees.AccessExpression, Trees.Synthetic {
    public abstract List<Object> accessor();

    @Override
    public String toString() {
      return accessor().toString();
    }
  }

  @Value.Immutable
  public interface InvokableDeclaration extends Named {
    List<Parameter> parameters();
  }

  @Value.Immutable
  public interface ValueDeclaration extends Named {
    Optional<TypeReference> type();
  }

  public interface Typed {
    TypeReference type();
  }

  @Value.Immutable
  public interface Parameter extends Named, Typed {}

  public interface Named {
    Identifier name();
  }

  public interface InvokableStatement {
    InvokableDeclaration declaration();
  }

  @Value.Immutable
  public interface Block extends TemplatePart {
    List<TemplatePart> parts();
  }

  @Value.Immutable(singleton = true, builder = false)
  public interface Comment extends UnitPart, TemplatePart {}

  @Value.Immutable
  public interface ConditionalBlock extends Conditional, Block, Synthetic {}

  @Value.Immutable
  public interface IfStatement extends TemplatePart, Synthetic {
    ConditionalBlock then();

    List<ConditionalBlock> otherwiseIf();

    Optional<Block> otherwise();
  }

  @Value.Immutable
  public interface ForStatement extends Block, Synthetic {
    List<GeneratorDeclaration> declaration();
  }

  @Value.Immutable(builder = false)
  public interface ForIterationAccessExpression extends AccessExpression {
    @Value.Parameter
    AccessExpression access();
  }

  @Value.Immutable
  public interface LetStatement extends Block, InvokableStatement, Synthetic {}

  @Value.Immutable
  public interface InvokeStatement extends Block, Synthetic {
    Expression access();

    List<Expression> params();
  }

  public interface DirectiveStart extends Directive {}

  public interface DirectiveEnd extends Directive {}

  @Value.Immutable(singleton = true, builder = false)
  public interface LetEnd extends DirectiveEnd {}

  @Value.Immutable(singleton = true, builder = false)
  public interface ForEnd extends DirectiveEnd {}

  @Value.Immutable(singleton = true, builder = false)
  public interface IfEnd extends DirectiveEnd {}

  @Value.Immutable(builder = false)
  public interface InvokeEnd extends DirectiveEnd {
    @Value.Parameter
    AccessExpression access();
  }

  @Value.Immutable(builder = false)
  public interface InvokeString extends DirectiveStart {
    @Value.Parameter
    StringLiteral literal();
  }

  public interface InvokeDeclaration {
    AccessExpression access();

    Optional<ApplyExpression> invoke();
  }

  @Value.Immutable
  public interface Invoke extends InvokeDeclaration, DirectiveStart {}

  public interface Directive extends TemplatePart {}

  @Value.Immutable
  public interface Let extends DirectiveStart, InvokableStatement {}

  public interface UnitPart {}

  @Value.Immutable
  public interface Unit {
    List<UnitPart> parts();
  }

  public interface TemplatePart {}

  /**
   * Non parser generated expressions or statements, produced by AST transformations, typing etc.
   */
  public interface Synthetic {}

  @Value.Immutable
  public interface Template extends Directive, Block, UnitPart, InvokableStatement {}

  public interface Expression {}

  @Value.Immutable
  public interface AccessExpression extends Expression {
    List<Identifier> path();
  }

  @Value.Immutable
  public interface ApplyExpression extends Expression {
    List<Expression> params();
  }

  public interface GeneratorDeclaration {
    ValueDeclaration declaration();

    Expression from();
  }

  @Value.Immutable
  public interface AssignGenerator extends GeneratorDeclaration {}

  @Value.Immutable
  public interface IterationGenerator extends GeneratorDeclaration {
    Optional<Expression> condition();
  }

  @Value.Immutable
  public interface For extends DirectiveStart {
    List<GeneratorDeclaration> declaration();
  }

  @Value.Immutable
  public interface If extends Conditional, DirectiveStart {}

  public interface Otherwise extends DirectiveStart {}

  @Value.Immutable
  public interface ElseIf extends Otherwise, Conditional {}

  public interface Conditional {
    Expression condition();
  }

  @Value.Immutable(singleton = true, builder = false)
  public interface Else extends Otherwise {}

  @Value.Immutable(singleton = true, builder = false)
  public interface TemplateEnd extends DirectiveEnd, Synthetic {}

  public interface TextPart {}

  @Value.Immutable(builder = false)
  public static abstract class StringLiteral implements Expression {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return StringLiterals.toLiteral(value());
    }
  }

  @Value.Immutable(singleton = true, builder = false)
  public static abstract class Newline implements TextPart {
    @Override
    public String toString() {
      return StringLiterals.toLiteral("\n");
    }
  }

  @Value.Immutable(builder = false)
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

  @Value.Immutable
  public interface TextBlock extends TemplatePart {
    List<TextPart> parts();
  }

  @Value.Immutable
  public static abstract class TextLine implements TemplatePart, Synthetic {
    public abstract TextFragment fragment();

    public boolean isBlank() {
      return fragment().isWhitespace();
    }

    public boolean isEmpty() {
      return fragment().value().isEmpty();
    }

    @Value.Default
    public boolean newline() {
      return false;
    }
  }

}
