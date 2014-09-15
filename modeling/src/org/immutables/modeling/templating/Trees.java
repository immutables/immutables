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

import org.immutables.modeling.LiteralEscapers;
import com.google.common.base.Optional;
import java.util.List;
import org.immutables.annotation.GenerateConstructorParameter;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateNested;
import org.immutables.annotation.GenerateParboiled;

/**
 * Abstract syntax trees.
 */
@GenerateNested
@GenerateParboiled
public class Trees {

  @GenerateImmutable(builder = false)
  public interface Identifier {
    @GenerateConstructorParameter
    String value();
  }

  @GenerateImmutable(builder = false)
  public interface TypeReference {
    @GenerateConstructorParameter
    String value();
  }

  @GenerateImmutable
  public interface InvokableDeclaration extends Named {
    List<Parameter> parameters();
  }

  @GenerateImmutable
  public interface ValueDeclaration extends Named, Typed {}

  @GenerateImmutable
  public interface Parameter extends Named, Typed {}

  public interface Typed {
    Optional<TypeReference> type();
  }

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

  @GenerateImmutable
  public interface ConditionalBlock extends Conditional, Block, SyntheticStatement {}

  @GenerateImmutable
  public interface IfStatement extends SyntheticStatement {
    ConditionalBlock then();

    List<ConditionalBlock> otherwiseIf();

    Optional<Block> otherwise();
  }

  @GenerateImmutable
  public interface ForStatement extends Block, SyntheticStatement {
    List<GeneratorDeclaration> declaration();
  }

  @GenerateImmutable
  public interface LetStatement extends Block, SyntheticStatement, InvokableStatement {}

  @GenerateImmutable
  public interface InvokeStatement extends Block, SyntheticStatement, InvokeDeclaration {}

  public interface DirectiveStart extends Directive {}

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

    Optional<InvokeExpression> invoke();
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

  public interface SyntheticStatement extends TemplatePart {}

  @GenerateImmutable
  public interface Template extends Directive, UnitPart, Block, InvokableStatement {}

  public interface Expression {}

  @GenerateImmutable
  public interface AccessExpression extends Expression {
    List<Identifier> path();
  }

  @GenerateImmutable
  public interface InvokeExpression extends Expression {
    List<Expression> params();
  }

  public interface GeneratorDeclaration {
    ValueDeclaration declaration();

    Expression from();
  }

  @GenerateImmutable
  public interface AssignGenerator extends GeneratorDeclaration {}

  @GenerateImmutable
  public interface IterationGenerator extends GeneratorDeclaration, Conditional {}

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
  public interface TemplateEnd extends SyntheticStatement, DirectiveEnd {}

  public interface TextPart {}

  @GenerateImmutable(singleton = true, builder = false)
  public static abstract class Newline implements TextPart {
    @Override
    public String toString() {
      return LiteralEscapers.toLiteral("\n");
    }
  }

  @GenerateImmutable(builder = false)
  public static abstract class TextFragment implements TextPart {
    @GenerateConstructorParameter
    public abstract String value();

    @Override
    public String toString() {
      return LiteralEscapers.toLiteral(value());
    }
  }

  @GenerateImmutable
  public interface TextBlock extends TemplatePart {
    List<TextPart> parts();
  }
}
