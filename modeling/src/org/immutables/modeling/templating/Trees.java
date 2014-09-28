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
package org.immutables.modeling.templating;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import java.util.List;
import org.immutables.annotation.GenerateConstructorParameter;
import org.immutables.annotation.GenerateDefault;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateNested;
import org.immutables.annotation.GenerateParboiled;
import org.immutables.annotation.GenerateTransformer;
import org.immutables.modeling.common.StringLiterals;

/**
 * Abstract syntax trees.
 */
@GenerateNested
@GenerateParboiled
@GenerateTransformer
public class Trees {

  @GenerateImmutable(builder = false)
  public static abstract class Identifier {
    @GenerateConstructorParameter
    public abstract String value();

    @Override
    public String toString() {
      return "`" + value() + "`";
    }
  }

  @GenerateImmutable(builder = false)
  public static abstract class TypeIdentifier {
    @GenerateConstructorParameter
    public abstract String value();

    @Override
    public String toString() {
      return "`" + value() + "`";
    }
  }

  public interface TypeReference {}

  @GenerateImmutable
  public static abstract class TypeDeclaration implements TypeReference {
    public abstract TypeIdentifier type();

    @GenerateDefault
    public Kind kind() {
      return Kind.SCALAR;
    }

    public enum Kind {
      SCALAR,
      ITERABLE
    }
  }

  @GenerateImmutable
  public abstract static class ResolvedType implements Trees.TypeReference, Trees.Synthetic {
    @GenerateConstructorParameter
    public abstract Object type();

    @Override
    public String toString() {
      return type().toString();
    }
  }

  @GenerateImmutable
  public abstract static class BoundAccessExpression implements Trees.AccessExpression, Trees.Synthetic {
    public abstract List<Object> accessor();

    @Override
    public String toString() {
      return accessor().toString();
    }
  }

  @GenerateImmutable
  public interface InvokableDeclaration extends Named {
    List<Parameter> parameters();
  }

  @GenerateImmutable
  public interface ValueDeclaration extends Named {
    Optional<TypeReference> type();
  }

  public interface Typed {
    TypeReference type();
  }

  @GenerateImmutable
  public interface Parameter extends Named, Typed {}

  public interface Named {
    Identifier name();
  }

  public interface InvokableStatement {
    InvokableDeclaration declaration();
  }

  @GenerateImmutable
  public interface Block extends TemplatePart {
    List<TemplatePart> parts();
  }

  @GenerateImmutable(singleton = true, builder = false)
  public interface Comment extends UnitPart, TemplatePart {}

  @GenerateImmutable
  public interface ConditionalBlock extends Conditional, Block, Synthetic {}

  @GenerateImmutable
  public interface IfStatement extends TemplatePart, Synthetic {
    ConditionalBlock then();

    List<ConditionalBlock> otherwiseIf();

    Optional<Block> otherwise();
  }

  @GenerateImmutable
  public interface ForStatement extends Block, Synthetic {
    List<GeneratorDeclaration> declaration();
  }

  @GenerateImmutable(builder = false)
  public interface ForIterationAccessExpression extends AccessExpression {
    @GenerateConstructorParameter
    AccessExpression access();
  }

  @GenerateImmutable
  public interface LetStatement extends Block, InvokableStatement, Synthetic {}

  @GenerateImmutable
  public interface InvokeStatement extends Block, Synthetic {
    Optional<TextFragment> whitespace();

    Expression access();

    List<Expression> params();
  }

  public interface DirectiveStart extends Directive {
    Optional<TextFragment> before();

    Directive withBefore(TextFragment fragment);
  }

  public interface DirectiveEnd extends Directive {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface LetEnd extends DirectiveEnd {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface ForEnd extends DirectiveEnd {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface IfEnd extends DirectiveEnd {}

  @GenerateImmutable(builder = false)
  public interface InvokeEnd extends DirectiveEnd {
    @GenerateConstructorParameter
    AccessExpression access();
  }

  public interface InvokeDeclaration {
    AccessExpression access();

    Optional<ApplyExpression> invoke();
  }

  @GenerateImmutable
  public interface Invoke extends InvokeDeclaration, DirectiveStart {}

  public interface Directive extends TemplatePart {}

  @GenerateImmutable
  public interface Let extends DirectiveStart, InvokableStatement {}

  public interface UnitPart {}

  @GenerateImmutable
  public interface Unit {
    List<UnitPart> parts();
  }

  public interface TemplatePart {}

  /**
   * Non parser generated expressions or statements, produced by AST transformations, typing etc.
   */
  public interface Synthetic {}

  @GenerateImmutable
  public interface Template extends Directive, Block, UnitPart, InvokableStatement {}

  public interface Expression {}

  @GenerateImmutable
  public interface AccessExpression extends Expression {
    List<Identifier> path();
  }

  @GenerateImmutable
  public interface ApplyExpression extends Expression {
    List<Expression> params();
  }

  public interface GeneratorDeclaration {
    ValueDeclaration declaration();

    Expression from();
  }

  @GenerateImmutable
  public interface AssignGenerator extends GeneratorDeclaration {}

  @GenerateImmutable
  public interface IterationGenerator extends GeneratorDeclaration {
    Optional<Expression> condition();
  }

  @GenerateImmutable
  public interface For extends DirectiveStart {
    List<GeneratorDeclaration> declaration();
  }

  @GenerateImmutable
  public interface If extends Conditional, DirectiveStart {}

  public interface Otherwise extends DirectiveStart {}

  @GenerateImmutable
  public interface ElseIf extends Otherwise, Conditional {}

  public interface Conditional {
    Expression condition();
  }

  @GenerateImmutable(singleton = true, builder = false)
  public interface Else extends Otherwise {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface TemplateEnd extends DirectiveEnd, Synthetic {}

  public interface TextPart {}

  @GenerateImmutable(singleton = true, builder = false)
  public static abstract class Newline implements TextPart {
    @Override
    public String toString() {
      return StringLiterals.toLiteral("\n");
    }
  }

  @GenerateImmutable(builder = false)
  public static abstract class TextFragment implements TextPart {
    @GenerateConstructorParameter
    public abstract String value();

    @Override
    public String toString() {
      return StringLiterals.toLiteral(value());
    }

    public boolean isWhitespace() {
      return CharMatcher.WHITESPACE.matchesAllOf(value());
    }
  }

  @GenerateImmutable
  public interface TextBlock extends TemplatePart {
    List<TextPart> parts();
  }

  @GenerateImmutable
  public static abstract class TextLine implements TemplatePart, Synthetic {
    public abstract TextFragment fragment();

    public boolean isBlank() {
      return fragment().isWhitespace();
    }

    @GenerateDefault
    public boolean newline() {
      return false;
    }
  }

}
