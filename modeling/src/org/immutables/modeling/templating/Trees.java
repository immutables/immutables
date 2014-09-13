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
  public interface ValueDeclaration extends Named, TypeAscriped {}

  @GenerateImmutable
  public interface Parameter extends Named, TypeAscriped {}

  public interface TypeAscriped {
    Optional<TypeReference> type();
  }

  public interface Named {
    Identifier name();
  }

  public interface DirectiveStart extends Directive, TemplatePart {}

  public interface DirectiveEnd extends Directive, TemplatePart {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface LetEnd extends DirectiveEnd {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface ForEnd extends DirectiveEnd {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface IfEnd extends DirectiveEnd {}

  @GenerateImmutable
  public interface InvokeEnd extends DirectiveEnd {
    AccessExpression access();
  }

  @GenerateImmutable
  public interface InvokeStart extends DirectiveStart {
    AccessExpression access();

    InvokeExpression invoke();
  }

  public interface Directive {}

  @GenerateImmutable
  public interface Let extends DirectiveStart {
    InvokableDeclaration declaration();
  }

  public interface TopLevel {}

  @GenerateImmutable
  public interface Unit {
    List<TopLevel> parts();
  }

  public interface TemplatePart {}

  @GenerateImmutable
  public interface Template extends Directive, TopLevel {
    InvokableDeclaration declaration();

    List<TemplatePart> parts();
  }

  public interface TextPart {}

  @GenerateImmutable(singleton = true, builder = false)
  public interface Newline extends TextPart {}

  @GenerateImmutable(builder = false)
  public interface TextFragment extends TextPart {
    @GenerateConstructorParameter
    String value();
  }

  @GenerateImmutable
  public interface TextBlock extends TemplatePart {
    List<TextPart> parts();
  }

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
  public interface If extends Condition, DirectiveStart {}

  @GenerateImmutable
  public interface ElseIf extends Condition, DirectiveStart {}

  public interface Condition {
    Expression condition();
  }

  @GenerateImmutable(singleton = true, builder = false)
  public interface Else extends DirectiveStart {}

  @GenerateImmutable
  public interface AssignGenerator extends GeneratorDeclaration {}

  @GenerateImmutable
  public interface IterationGenerator extends GeneratorDeclaration, Condition {}

  @GenerateImmutable
  public interface For extends DirectiveStart {
    List<GeneratorDeclaration> declaration();
  }
}
