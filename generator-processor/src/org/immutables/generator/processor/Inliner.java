/*
   Copyright 2015 Immutables Authors and Contributors

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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.generator.processor.ImmutableTrees.AssignGenerator;
import org.immutables.generator.processor.ImmutableTrees.ForStatement;
import org.immutables.generator.processor.ImmutableTrees.Identifier;
import org.immutables.generator.processor.ImmutableTrees.InvokableDeclaration;
import org.immutables.generator.processor.ImmutableTrees.InvokeStatement;
import org.immutables.generator.processor.ImmutableTrees.LetStatement;
import org.immutables.generator.processor.ImmutableTrees.SimpleAccessExpression;
import org.immutables.generator.processor.ImmutableTrees.Template;
import org.immutables.generator.processor.ImmutableTrees.TextLine;
import org.immutables.generator.processor.ImmutableTrees.Unit;
import org.immutables.generator.processor.ImmutableTrees.ValueDeclaration;

final class Inliner {
  private Inliner() {}

  private final Map<Trees.Identifier, InlinedStatementCreator> inlinables = Maps.newHashMap();

  public static Unit optimize(Unit unit) {
    return new Inliner().inline(unit);
  }

  private Unit inline(Unit unit) {
    new Finder().toUnit(unit);
    return new Weaver().toUnit(unit);
  }

  private static class InlinedStatementCreator extends TreesTransformer {
    private final Template inlinable;
    private final int uniqueSuffix;
    private final Set<Trees.Identifier> remapped = Sets.newHashSet();

    InlinedStatementCreator(Template inlinable) {
      this.uniqueSuffix = System.identityHashCode(inlinable);
      this.inlinable = inlinable;
      for (Trees.Parameter p : inlinable.declaration().parameters()) {
        remapped.add(p.name());
      }
    }

    ForStatement inlined(List<Trees.Expression> params, Iterable<? extends Trees.TemplatePart> bodyParts) {
      ForStatement.Builder builder = ForStatement.builder()
          .useForAccess(false)
          .useDelimit(false);

      Iterator<Trees.Parameter> formals = inlinable.declaration().parameters().iterator();

      for (Trees.Expression argument : params) {
        Trees.Parameter formal = formals.next();

        builder.addDeclaration(
            AssignGenerator.builder()
                .declaration(declarationFor(formal))
                .from(argument)
                .build());
      }

      addBodyIfNecessary(builder, params, bodyParts);

      builder.addAllParts(asTemplatePartsElements(inlinable, inlinable.parts()));

      return builder.build();
    }

    private ValueDeclaration declarationFor(Trees.Parameter formalParameter) {
      return ValueDeclaration.builder()
          .type(formalParameter.type())
          .name(remappedIdentifier(formalParameter.name()))
          .build();
    }

    private void addBodyIfNecessary(
        ForStatement.Builder builder,
        List<Trees.Expression> params,
        Iterable<? extends Trees.TemplatePart> bodyParts) {
      // body goes as one special parameter, don't handle other mismatches
      if (Iterables.isEmpty(bodyParts)) {
        return;
      }

      Preconditions.checkState(inlinable.declaration().parameters().size() == params.size() + 1);

      Trees.Parameter lastParameter = Iterables.getLast(inlinable.declaration().parameters());

      LetStatement.Builder letBuilder = LetStatement.builder()
          .addAllParts(bodyParts)
          .declaration(InvokableDeclaration.builder()
              .name(remappedIdentifier(lastParameter.name()))
              .build());

      remapped.add(lastParameter.name());
      builder.addParts(letBuilder.build());
    }

    @Override
    public SimpleAccessExpression toSimpleAccessExpression(SimpleAccessExpression value) {
      final Trees.Identifier topAccessIdentifier = value.path().get(0);
      if (remapped.contains(topAccessIdentifier)) {
        return new TreesTransformer() {
          @Override
          public Identifier toIdentifier(Identifier value) {
            return topAccessIdentifier == value
                ? remappedIdentifier(value)
                : value;
          }
        }.toSimpleAccessExpression(value);
      }
      return value;
    }

    protected Identifier remappedIdentifier(Trees.Identifier value) {
      return Identifier.of(
          value.value()
              + "_"
              + inlinable.declaration().name().value()
              + "_"
              + uniqueSuffix);
    }
  }

  final class Finder extends TreesTransformer {
    private boolean inlinable;

    @Override
    public Template toTemplate(Template value) {
      if (value.isPublic()) {
        return value;
      }

      inlinable = true;

      asTemplatePartsElements(value, value.parts());

      if (inlinable) {
        inlinables.put(value.declaration().name(), new InlinedStatementCreator(value));
      }
      return value;
    }

    @Override
    public InvokableDeclaration toInvokableDeclaration(InvokableDeclaration value) {
      inlinable = false;
      return value;
    }

    @Override
    public ValueDeclaration toValueDeclaration(ValueDeclaration value) {
      inlinable = false;
      return value;
    }

    @Override
    public TextLine toTextLine(TextLine value) {
      if (value.newline()) {
        inlinable = false;
      }
      return value;
    }
  }

  final class Weaver extends TreesTransformer {

    @Override
    protected Trees.TemplatePart asTemplatePart(InvokeStatement value) {
      @Nullable InlinedStatementCreator creator = tryGetInlinable(value);
      if (creator != null) {
        return creator.inlined(value.params(), value.parts());
      }
      return value;
    }

    private @Nullable InlinedStatementCreator tryGetInlinable(InvokeStatement invoke) {
      Trees.Expression access = invoke.access();
      if (access instanceof SimpleAccessExpression) {
        SimpleAccessExpression ref = (SimpleAccessExpression) access;
        if (ref.path().size() == 1) {
          Trees.Identifier identifier = ref.path().get(0);
          return inlinables.get(identifier);
        }
      }
      return null;
    }
  }
}
