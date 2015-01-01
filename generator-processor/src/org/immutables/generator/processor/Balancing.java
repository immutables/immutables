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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.generator.processor.ImmutableTrees.Block;
import org.immutables.generator.processor.ImmutableTrees.ConditionalBlock;
import org.immutables.generator.processor.ImmutableTrees.Else;
import org.immutables.generator.processor.ImmutableTrees.ElseIf;
import org.immutables.generator.processor.ImmutableTrees.For;
import org.immutables.generator.processor.ImmutableTrees.ForEnd;
import org.immutables.generator.processor.ImmutableTrees.ForStatement;
import org.immutables.generator.processor.ImmutableTrees.If;
import org.immutables.generator.processor.ImmutableTrees.IfEnd;
import org.immutables.generator.processor.ImmutableTrees.IfStatement;
import org.immutables.generator.processor.ImmutableTrees.Invoke;
import org.immutables.generator.processor.ImmutableTrees.InvokeEnd;
import org.immutables.generator.processor.ImmutableTrees.InvokeStatement;
import org.immutables.generator.processor.ImmutableTrees.Let;
import org.immutables.generator.processor.ImmutableTrees.LetEnd;
import org.immutables.generator.processor.ImmutableTrees.LetStatement;
import org.immutables.generator.processor.ImmutableTrees.Template;
import org.immutables.generator.processor.ImmutableTrees.TemplateEnd;
import org.immutables.generator.processor.ImmutableTrees.Unit;

public final class Balancing {
  private Balancing() {}

  public static Unit balance(Unit unit) {
    return TRANSFORMER.transform((Void) null, unit);
  }

  private static final TreesTransformer<Void> TRANSFORMER = new TreesTransformer<Void>() {
    @Override
    public Template transform(Void context, Template template) {
      return new TemplateScope(template).balance();
    }
  };

  private static abstract class Scope {
    List<Trees.TemplatePart> parts = Lists.newArrayList();

    final Scope pass(Trees.TemplatePart part) {
      if (part instanceof Trees.DirectiveStart) {
        return nextOrAdd((Trees.DirectiveStart) part);
      } else if (part instanceof Trees.DirectiveEnd) {
        return end((Trees.DirectiveEnd) part);
      } else if (incorrect(part)) {
        return correct(part);
      }
      add(part);
      return this;
    }

    Scope nextOrAdd(Trees.DirectiveStart part) {
      Scope next = next(part);
      if (next == this) {
        add(part);
      }
      return next;
    }

    /**
     * @param part
     */
    Scope correct(Trees.TemplatePart part) {
      return this;
    }

    /**
     * @param part
     */
    boolean incorrect(Trees.TemplatePart part) {
      return false;
    }

    void add(Trees.TemplatePart part) {
      parts.add(part);
    }

    abstract Scope end(Trees.DirectiveEnd directiveEnd);

    final Scope passAll(Iterable<Trees.TemplatePart> parts) {
      Scope scope = this;
      for (Trees.TemplatePart part : parts) {
        scope = scope.pass(part);
      }
      return scope;
    }

    final Scope next(Trees.DirectiveStart directive) {
      if (directive instanceof If) {
        return new IfScope(this, (If) directive);
      }
      if (directive instanceof For) {
        return new ForScope(this, (For) directive);
      }
      if (directive instanceof Let) {
        return new LetScope(this, (Let) directive);
      }
      if (directive instanceof Invoke) {
        return new InvokeScope(this, (Invoke) directive);
      }
      return this;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .addValue(parts)
          .toString();
    }
  }

  private static final class UnitScope extends Scope {
    @Override
    Scope end(Trees.DirectiveEnd directiveEnd) {
      return this;
    }

    @Override
    boolean incorrect(Trees.TemplatePart part) {
      return part == null;
    }

    @Override
    Scope correct(Trees.TemplatePart part) {
      return this;
    }
  }

  private static final class TemplateScope extends BlockScope {
    private final Template template;

    TemplateScope(Template template) {
      super(new UnitScope(), TemplateEnd.of(), true, false);
      this.template = template;
    }

    Template balance() {
      Scope scope = passAll(template.parts()).pass(TemplateEnd.of());
      if (scope != parent) {
        // TBD
        throw new MisplacedDirective(this, TemplateEnd.of());
      }
      return createPart();
    }

    @Override
    void add(Trees.TemplatePart part) {
      if (!(part instanceof TemplateEnd)) {
        super.add(part);
      }
    }

    @Override
    Template createPart() {
      return template.withParts(parts);
    }
  }

  private static final class MisplacedDirective extends RuntimeException {
    private final Trees.Directive directive;
    private final Scope scope;

    MisplacedDirective(Scope scope, Trees.Directive directive) {
      this.scope = scope;
      this.directive = directive;
    }

    @Override
    public String getMessage() {
      return "Misplaced directive: " + directive + " in " + scope;
    }
  }

  private static abstract class BlockScope extends Scope {
    private final Trees.DirectiveEnd expectedEnd;
    private final boolean requiresEnd;
    private final boolean sharesEnd;
    final Scope parent;

    BlockScope(
        Scope parent,
        Trees.DirectiveEnd expectedEnd,
        boolean requiresEnd,
        boolean sharesEnd) {
      this.parent = parent;
      this.expectedEnd = expectedEnd;
      this.requiresEnd = requiresEnd;
      this.sharesEnd = sharesEnd;
    }

    abstract Trees.TemplatePart createPart();

    @Override
    boolean incorrect(Trees.TemplatePart part) {
      return part instanceof Trees.Otherwise;
    }

    @Override
    Scope correct(Trees.TemplatePart part) {
      return splat(part);
    }

    @Override
    final Scope end(Trees.DirectiveEnd directiveEnd) {
      if (expectedEnd.equals(directiveEnd)) {
        Scope scope = parent.pass(createPart());
        return sharesEnd ? scope.end(directiveEnd) : scope;
      } else if (!requiresEnd) {
        return splat(directiveEnd);
      } else {
        throw new MisplacedDirective(this, directiveEnd);
      }
    }

    private Scope splat(Trees.TemplatePart part) {
      List<Trees.TemplatePart> parts = this.parts;
      this.parts = Lists.newArrayList();
      return parent.pass(createPart())
          .passAll(parts)
          .pass(part);
    }
  }

  private static class ForScope extends BlockScope {
    private final For directive;

    ForScope(Scope parent, For directive) {
      super(parent, ForEnd.of(), true, false);
      this.directive = directive;
    }

    @Override
    ForStatement createPart() {
      return ForStatement.builder()
          .addAllDeclaration(directive.declaration())
          .addAllParts(parts)
          .build();
    }
  }

  private static class LetScope extends BlockScope {
    private final Let directive;

    LetScope(Scope parent, Let directive) {
      super(parent, LetEnd.of(), true, false);
      this.directive = directive;
    }

    @Override
    LetStatement createPart() {
      return LetStatement.builder()
          .declaration(directive.declaration())
          .addAllParts(parts)
          .build();
    }
  }

  private static class InvokeScope extends BlockScope {
    private final Invoke directive;

    InvokeScope(Scope parent, Invoke directive) {
      super(parent, InvokeEnd.of(directive.access()), false, false);
      this.directive = directive;
    }

    @Override
    InvokeStatement createPart() {
      return InvokeStatement.builder()
          .access(directive.access())
          .addAllParams(
              directive.invoke().isPresent()
                  ? directive.invoke().get().params()
                  : ImmutableList.<Trees.Expression>of())
          .addAllParts(parts)
          .build();
    }
  }

  private static class IfScope extends BlockScope {
    private final If directive;
    private final IfStatement.Builder builder;
    @Nullable
    private ElseIf currentElseIf;
    @Nullable
    private Else currentElse;

    IfScope(Scope parent, If directive) {
      super(parent, IfEnd.of(), true, false);
      this.directive = directive;
      this.builder = IfStatement.builder();
    }

    @Override
    void add(Trees.TemplatePart part) {
      if (part instanceof ElseIf || part instanceof Else) {
        if (currentElse != null) {
          throw new MisplacedDirective(this, (Trees.Directive) part);
        }

        flushBlock();

        if (part instanceof ElseIf) {
          currentElseIf = (ElseIf) part;
        } else if (part instanceof Else) {
          currentElse = (Else) part;
        }
      } else {
        super.add(part);
      }
    }

    @Override
    boolean incorrect(Trees.TemplatePart part) {
      return false;
    }

    private void flushBlock() {
      if (currentElse != null) {
        builder.otherwise(Block.builder()
            .addAllParts(parts)
            .build());
      } else if (currentElseIf != null) {
        builder.addOtherwiseIf(ConditionalBlock.builder()
            .condition(currentElseIf.condition())
            .addAllParts(parts)
            .build());
      } else {
        builder.then(ConditionalBlock.builder()
            .condition(directive.condition())
            .addAllParts(parts)
            .build());
      }
      parts.clear();
    }

    @Override
    IfStatement createPart() {
      flushBlock();
      return builder.build();
    }
  }
}
