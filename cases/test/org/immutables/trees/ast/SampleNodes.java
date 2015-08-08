package org.immutables.trees.ast;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import java.util.List;
import org.immutables.generator.StringLiterals;
import org.immutables.trees.Trees;
import org.immutables.value.Value;

/**
 * This real-world AST nodes class helps to resolve compilation problems
 * when developing transformers template. So we leave it here as a compilation test.
 */
@Trees.Ast
@Trees.Transform
@Value.Enclosing
@SuppressWarnings("immutables")
public interface SampleNodes {

  @Value.Immutable(builder = false)
  public static abstract class Identifier {
    @Value.Parameter
    public abstract String value();

    @Override
    public String toString() {
      return "`" + value() + "`";
    }
  }

  @Value.Immutable
  public interface InvokableDeclaration extends Named {
    List<Parameter> parameters();
  }

  @Value.Immutable
  public interface ValueDeclaration extends Named {}

  @Value.Immutable
  public interface Parameter extends Named {}

  public interface Named {
    Identifier name();
  }

  public interface InvokableStatement {
    InvokableDeclaration declaration();
  }

  public interface Block extends TemplatePart {
    List<TemplatePart> parts();
  }

  @Value.Immutable
  public interface PureBlock extends TemplatePart {
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

  public interface GeneratorDeclaration {}

  public interface GeneratorValueDeclaration extends GeneratorDeclaration {
    ValueDeclaration declaration();

    Expression from();
  }

  @Value.Immutable
  public interface AssignGenerator extends GeneratorValueDeclaration {}

  @Value.Immutable
  public interface IterationGenerator extends GeneratorValueDeclaration {
    Optional<Expression> condition();
  }

  @Value.Immutable
  public interface TransformGenerator extends GeneratorValueDeclaration {
    Expression transform();

    ValueDeclaration varDeclaration();

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
