package org.immutables.modeling.templating;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.modeling.templating.ImmutableTrees.Block;
import org.immutables.modeling.templating.ImmutableTrees.ConditionalBlock;
import org.immutables.modeling.templating.ImmutableTrees.Else;
import org.immutables.modeling.templating.ImmutableTrees.ElseIf;
import org.immutables.modeling.templating.ImmutableTrees.For;
import org.immutables.modeling.templating.ImmutableTrees.ForEnd;
import org.immutables.modeling.templating.ImmutableTrees.ForStatement;
import org.immutables.modeling.templating.ImmutableTrees.If;
import org.immutables.modeling.templating.ImmutableTrees.IfEnd;
import org.immutables.modeling.templating.ImmutableTrees.IfStatement;
import org.immutables.modeling.templating.ImmutableTrees.Invoke;
import org.immutables.modeling.templating.ImmutableTrees.InvokeEnd;
import org.immutables.modeling.templating.ImmutableTrees.InvokeStatement;
import org.immutables.modeling.templating.ImmutableTrees.Let;
import org.immutables.modeling.templating.ImmutableTrees.LetEnd;
import org.immutables.modeling.templating.ImmutableTrees.LetStatement;
import org.immutables.modeling.templating.ImmutableTrees.Template;
import org.immutables.modeling.templating.ImmutableTrees.TemplateEnd;
import org.immutables.modeling.templating.ImmutableTrees.Unit;
import org.immutables.modeling.templating.Trees.Directive;
import org.immutables.modeling.templating.Trees.DirectiveEnd;
import org.immutables.modeling.templating.Trees.DirectiveStart;
import org.immutables.modeling.templating.Trees.Expression;
import org.immutables.modeling.templating.Trees.Otherwise;
import org.immutables.modeling.templating.Trees.SyntheticStatement;
import org.immutables.modeling.templating.Trees.TemplatePart;

public final class Balancing {
  private Balancing() {}

  public static Function<Unit, Unit> transformer() {
    return TRANSFORMER;
  }

  private static final UnitTransformer TRANSFORMER = new UnitTransformer() {
    @Override
    Template transformTemplate(Template template) {
      return new TemplateScope(template).balance();
    }
  };

  private static abstract class Scope {
    List<TemplatePart> parts = Lists.newArrayList();

    final Scope pass(TemplatePart part) {
      if (part instanceof DirectiveStart) {
        return next((DirectiveStart) part);
      } else if (part instanceof DirectiveEnd) {
        return end((DirectiveEnd) part);
      } else if (incorrect(part)) {
        return correct(part);
      }
      add(part);
      return this;
    }

    /**
     * @param part
     */
    Scope correct(TemplatePart part) {
      return this;
    }

    /**
     * @param part
     */
    boolean incorrect(TemplatePart part) {
      return false;
    }

    void add(TemplatePart part) {
      parts.add(part);
    }

    abstract Scope end(DirectiveEnd directiveEnd);

    final Scope passAll(Iterable<TemplatePart> parts) {
      Scope scope = this;
      for (TemplatePart part : parts) {
        scope = scope.pass(part);
      }
      return scope;
    }

    final Scope next(DirectiveStart directive) {
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
  }

  private static final class TemplateScope extends Scope {
    final Template template;

    TemplateScope(Template template) {
      this.template = template;
    }

    @Override
    Scope end(DirectiveEnd directiveEnd) {
      throw new MisplacedDirective(this, directiveEnd);
    }

    public Template balance() {
      Scope scope = passAll(template.parts());
      if (scope != this) {
        // TBD
        throw new MisplacedDirective(this, TemplateEnd.of());
      }
      return template.withParts(parts);
    }

    @Override
    void add(TemplatePart part) {
      if (!(part instanceof TemplateEnd)) {
        super.add(part);
      }
    }
  }

  private static final class MisplacedDirective extends RuntimeException {
    final Directive directive;
    private final Scope scope;

    MisplacedDirective(Scope scope, Directive directive) {
      this.scope = scope;
      this.directive = directive;
    }

    @Override
    public String getMessage() {
      return "Misplaced directive: " + directive + " in " + scope;
    }
  }

  private static abstract class BlockScope extends Scope {
    final DirectiveEnd expectedEnd;
    final boolean requiresEnd;
    final boolean sharesEnd;
    final Scope parent;

    BlockScope(
        Scope parent,
        DirectiveEnd expectedEnd,
        boolean requiresEnd,
        boolean sharesEnd) {
      this.parent = parent;
      this.expectedEnd = expectedEnd;
      this.requiresEnd = requiresEnd;
      this.sharesEnd = sharesEnd;
    }

    abstract SyntheticStatement createPart();

    @Override
    boolean incorrect(TemplatePart part) {
      return part instanceof Otherwise;
    }

    @Override
    Scope correct(TemplatePart part) {
      return splat(part);
    }

    @Override
    final Scope end(DirectiveEnd directiveEnd) {
      if (expectedEnd.equals(directiveEnd)) {
        Scope scope = parent.pass(createPart());
        return sharesEnd ? scope.end(directiveEnd) : scope;
      } else if (!requiresEnd) {
        return splat(directiveEnd);
      } else {
        throw new MisplacedDirective(this, directiveEnd);
      }
    }

    private Scope splat(TemplatePart part) {
      List<TemplatePart> parts = this.parts;
      this.parts = Lists.newArrayList();
      return parent.pass(createPart())
          .passAll(parts)
          .pass(part);
    }
  }

  private static class ForScope extends BlockScope {
    private final For directive;

    public ForScope(Scope parent, For directive) {
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

    public LetScope(Scope parent, Let directive) {
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

    public InvokeScope(Scope parent, Invoke directive) {
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
                  : ImmutableList.<Expression>of())
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

    public IfScope(Scope parent, If directive) {
      super(parent, IfEnd.of(), true, false);
      this.directive = directive;
      this.builder = IfStatement.builder();
    }

    @Override
    void add(TemplatePart part) {
      if (part instanceof ElseIf || part instanceof Else) {
        if (currentElse != null) {
          throw new MisplacedDirective(this, (Directive) part);
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
    boolean incorrect(TemplatePart part) {
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
