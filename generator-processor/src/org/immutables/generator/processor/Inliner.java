package org.immutables.generator.processor;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.generator.processor.ImmutableTrees.AccessExpression;
import org.immutables.generator.processor.ImmutableTrees.AssignGenerator;
import org.immutables.generator.processor.ImmutableTrees.ForStatement;
import org.immutables.generator.processor.ImmutableTrees.InvokableDeclaration;
import org.immutables.generator.processor.ImmutableTrees.InvokeStatement;
import org.immutables.generator.processor.ImmutableTrees.LetStatement;
import org.immutables.generator.processor.ImmutableTrees.Template;
import org.immutables.generator.processor.ImmutableTrees.TextLine;
import org.immutables.generator.processor.ImmutableTrees.Unit;
import org.immutables.generator.processor.ImmutableTrees.ValueDeclaration;
import org.immutables.generator.processor.Trees.Expression;
import org.immutables.generator.processor.Trees.Identifier;
import org.immutables.generator.processor.Trees.Parameter;
import org.immutables.generator.processor.Trees.TemplatePart;
import org.immutables.generator.processor.Trees.UnitPart;

final class Inliner {
  private Inliner() {}

  private final Map<Identifier, InlinedStatementCreator> inlinables = Maps.newHashMap();

  public static Unit optimize(Unit unit) {
    return new Inliner().inline(unit);
  }

  private Unit inline(Unit unit) {
    new Finder().transform((Void) null, unit);
    return new Weaver().transform((Void) null, unit);
  }

  private static class InlinedStatementCreator extends TreesTransformer<Void> {
    private final Template inlinable;
    private final int uniqueSuffix;
    private final Set<Identifier> remapped = Sets.newHashSet();

    InlinedStatementCreator(Template inlinable) {
      this.uniqueSuffix = System.identityHashCode(inlinable);
      this.inlinable = inlinable;
      for (Parameter p : inlinable.declaration().parameters()) {
        remapped.add(p.name());
      }
    }

    ForStatement inlined(List<Expression> params, Iterable<? extends TemplatePart> bodyParts) {
      ForStatement.Builder builder = ForStatement.builder()
          .useForAccess(false)
          .useDelimit(false);

      Iterator<Parameter> formals = inlinable.declaration().parameters().iterator();

      for (Expression argument : params) {
        Parameter formal = formals.next();

        builder.addDeclaration(
            AssignGenerator.builder()
                .declaration(declarationFor(formal))
                .from(argument)
                .build());
      }

      addBodyIfNecessary(builder, params, bodyParts);

      builder.addAllParts(transformTemplateListParts((Void) null, inlinable, inlinable.parts()));

      return builder.build();
    }

    private ValueDeclaration declarationFor(Parameter formalParameter) {
      return ValueDeclaration.builder()
          .type(formalParameter.type())
          .name(remappedIdentifier(formalParameter.name()))
          .build();
    }

    private void addBodyIfNecessary(
        ForStatement.Builder builder,
        List<Expression> params,
        Iterable<? extends TemplatePart> bodyParts) {
      // body goes as one special parameter, don't handle other mismatches
      if (Iterables.isEmpty(bodyParts)) {
        return;
      }

      Preconditions.checkState(inlinable.declaration().parameters().size() == params.size() + 1);

      Parameter lastParameter = Iterables.getLast(inlinable.declaration().parameters());

      LetStatement.Builder letBuilder = LetStatement.builder()
          .addAllParts(bodyParts)
          .declaration(InvokableDeclaration.builder()
              .name(remappedIdentifier(lastParameter.name()))
              .build());

      remapped.add(lastParameter.name());
      builder.addParts(letBuilder.build());
    }

    @Override
    public AccessExpression transform(Void context, AccessExpression value) {
      final Identifier topAccessIdentifier = value.path().get(0);
      if (remapped.contains(topAccessIdentifier)) {
        return new TreesTransformer<Void>() {
          @Override
          public ImmutableTrees.Identifier transform(Void context, ImmutableTrees.Identifier value) {
            return topAccessIdentifier == value
                ? remappedIdentifier(value)
                : value;
          }
        }.transform(context, value);
      }
      return value;
    }

    protected ImmutableTrees.Identifier remappedIdentifier(Trees.Identifier value) {
      return ImmutableTrees.Identifier.of(value.value() + "_" + uniqueSuffix);
    }
  }

  final class Finder extends TreesTransformer<Void> {
    private boolean containsNewlines;

    @Override
    public Template transform(Void context, Template value) {
      if (value.isPublic()) {
        return value;
      }

      containsNewlines = false;

      super.transform(context, value);

      if (!containsNewlines) {
        inlinables.put(value.declaration().name(), new InlinedStatementCreator(value));
      }
      return value;
    }

    @Override
    public TextLine transform(Void context, TextLine value) {
      if (value.newline()) {
        containsNewlines = true;
      }
      return value;
    }
  }

  final class Weaver extends TreesTransformer<Void> {

    @Override
    protected Iterable<UnitPart> transformUnitListParts(Void context, Unit value, List<UnitPart> parts) {
      // TODO decide if we need to remove inlined completely
      // could be referenced by outer templates
      // return super.transformUnitListParts(context, value, parts);
      return super.transformUnitListParts(context, value, inlinedRemoved(parts));
    }

    private List<UnitPart> inlinedRemoved(List<UnitPart> parts) {
      List<UnitPart> newParts = Lists.newArrayListWithCapacity(parts.size());
      for (UnitPart p : parts) {
        if (!inlinables.containsKey(p)) {
          newParts.add(p);
        }
      }
      return newParts;
    }

    @Override
    protected TemplatePart transformTemplatePart(Void context, InvokeStatement value) {
      @Nullable
      InlinedStatementCreator creator = tryGetInlinable(value);
      if (creator != null) {
        return creator.inlined(value.params(), value.parts());
      }
      return value;
    }

    private @Nullable InlinedStatementCreator tryGetInlinable(InvokeStatement invoke) {
      Expression access = invoke.access();
      if (access instanceof AccessExpression) {
        AccessExpression ref = (AccessExpression) access;
        if (ref.path().size() == 1) {
          Identifier identifier = ref.path().get(0);
          return inlinables.get(identifier);
        }
      }
      return null;
    }
  }
}
