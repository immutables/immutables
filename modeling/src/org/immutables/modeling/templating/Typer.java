package org.immutables.modeling.templating;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.immutables.modeling.templating.ImmutableTrees.ApplyExpression;
import org.immutables.modeling.templating.Trees.Expression;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.immutables.annotation.GenerateConstructorParameter;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateNested;
import org.immutables.modeling.introspect.Accessors.BoundAccess;
import org.immutables.modeling.introspect.SwissArmyKnife;
import org.immutables.modeling.templating.ImmutableTrees.AccessExpression;
import org.immutables.modeling.templating.ImmutableTrees.AssignGenerator;
import org.immutables.modeling.templating.ImmutableTrees.ForStatement;
import org.immutables.modeling.templating.ImmutableTrees.IterationGenerator;
import org.immutables.modeling.templating.ImmutableTrees.LetStatement;
import org.immutables.modeling.templating.ImmutableTrees.Parameter;
import org.immutables.modeling.templating.ImmutableTrees.Template;
import org.immutables.modeling.templating.ImmutableTrees.TypeDeclaration;
import org.immutables.modeling.templating.ImmutableTrees.Unit;

@GenerateNested
public final class Typer {
  private final SwissArmyKnife knife;

  public Typer(SwissArmyKnife knife) {
    this.knife = knife;
  }

  public static class TypingException extends RuntimeException {
    TypingException(String message) {
      super(message);
    }
  }

  public Unit resolve(Unit unit) {
    return new Transformer().transform(new Scope(), unit);
  }

  @GenerateImmutable
  public interface ResolvedType extends Trees.TypeReference, Trees.Synthetic {
    @GenerateConstructorParameter
    TypeMirror type();
  }

  @GenerateImmutable
  public abstract static class BoundAccessExpression implements Trees.AccessExpression, Trees.Synthetic {
    public abstract List<BoundAccess> accessor();

    @Override
    public String toString() {
      return accessor().toString();
    }
  }

  private class Scope {
    private final Map<String, TypeMirror> locals = Maps.newLinkedHashMap();

    Scope nest() {
      Scope nested = new Scope();
      nested.locals.putAll(locals);
      return nested;
    }

    ImmutableTyper.ResolvedType declare(TypeDeclaration type, Trees.Identifier name) {
      if (isDeclared(name)) {
        throw new TypingException(String.format("Redeclaration of local %s", name));
      }
      return declare(resolve(type), name);
    }

    private boolean isDeclared(Trees.Identifier name) {
      return locals.containsKey(name.value());
    }

    private ImmutableTyper.ResolvedType declare(TypeMirror type, Trees.Identifier name) {
      locals.put(name.value(), type);
      return ImmutableTyper.ResolvedType.of(type);
    }

    private TypeMirror resolve(TypeDeclaration type) {
      TypeMirror resolved = knife.imports.get(type.type().value());
      if (resolved == null) {
        throw new TypingException(String.format("Could not resolve %s simple type", type));
      }
      if (type.kind() == Trees.TypeDeclaration.Kind.ITERABLE) {
        resolved = makeIterableTypeOf(resolved);
      }
      return resolved;
    }

    private DeclaredType makeIterableTypeOf(TypeMirror resolved) {
      return knife.types.getDeclaredType(knife.accessors.iterableElement, resolved);
    }

    BoundAccessExpression resolveAccess(AccessExpression expression) {
      ImmutableTyper.BoundAccessExpression.Builder builder =
          ImmutableTyper.BoundAccessExpression.builder()
              .addAllPath(expression.path());

      BoundAccess accessor = null;
      for (Trees.Identifier identifier : expression.path()) {
        accessor = bindAccess(accessor, identifier.value());
        builder.addAccessor(accessor);
      }

      return builder.build();
    }

    BoundAccess bindAccess(@Nullable BoundAccess previous, String name) {
      if (previous != null) {
        return knife.binder.bind(previous.type, name);
      }
      return knife.binder.bindLocalOrThis(knife.type.asType(), name, locals);
    }

    Trees.ValueDeclaration inferType(
        ImmutableTrees.ValueDeclaration declaration,
        Trees.Expression expression,
        Trees.TypeDeclaration.Kind kind) {

      if (expression instanceof BoundAccessExpression) {
        BoundAccessExpression scopeBoundAccess = (BoundAccessExpression) expression;
        BoundAccess lastAccess = Iterables.getLast(scopeBoundAccess.accessor());

        if (kind == Trees.TypeDeclaration.Kind.ITERABLE) {
          if (!lastAccess.isContainer()) {
            throw new TypingException(String.format("Not iterable type '%s'%n\tin expression '%s'",
                lastAccess.type,
                scopeBoundAccess.path()));
          }
        }

        if (declaration.type().isPresent()) {
          // TBD check type here if it's present
        } else {
          TypeMirror resolved = (kind == Trees.TypeDeclaration.Kind.ITERABLE)
              ? lastAccess.containedType
              : lastAccess.type;

          return declaration.withType(declare(resolved, declaration.name()));
        }
      } else {
        if (!declaration.type().isPresent()) {
          throw new TypingException(String.format("Value should be typed '%s'%n\texpression '%s'",
              declaration.name(),
              expression));
        }
      }

      return declaration;
    }
  }

  private static final class Transformer extends TreesTransformer<Scope> {

    @Override
    public AssignGenerator transform(Scope scope, AssignGenerator value) {
      AssignGenerator generator = super.transform(scope, value);
      return generator.withDeclaration(
          scope.inferType(
              (ImmutableTrees.ValueDeclaration) generator.declaration(),
              generator.from(),
              Trees.TypeDeclaration.Kind.SCALAR));
    }

    @Override
    public IterationGenerator transform(Scope scope, IterationGenerator value) {
      IterationGenerator generator = super.transform(scope, value);
      return generator.withDeclaration(
          scope.inferType(
              (ImmutableTrees.ValueDeclaration) generator.declaration(),
              generator.from(),
              Trees.TypeDeclaration.Kind.ITERABLE))
          .withCondition(transformIterationGeneratorConditionAfterDeclaration(scope, generator, generator.condition()));
    }

    private Optional<Expression> transformIterationGeneratorConditionAfterDeclaration(
        Scope scope,
        IterationGenerator generator,
        Optional<Expression> condition) {
      if (condition.isPresent()) {
        // Calling actual transformation
        return Optional.of(super.transformIterationGeneratorConditionElement(scope, generator, condition.get()));
      }
      return Optional.absent();
    }

    @Override
    protected Expression transformIterationGeneratorConditionElement(
        Scope scope,
        IterationGenerator value,
        Expression element) {
      // We prevent transformation here to manually do it after variable declaration is done
      return element;
    }

    @Override
    public Parameter transform(Scope scope, Parameter parameter) {
      return parameter.withType(
          scope.declare(
              (TypeDeclaration) parameter.type(),
              parameter.name()));
    }

    @Override
    public Template transform(Scope scope, Template template) {
      return super.transform(scope.nest(), template);
    }

    @Override
    public LetStatement transform(Scope scope, LetStatement value) {
      return super.transform(scope.nest(), value);
    }

    @Override
    public ForStatement transform(Scope scope, ForStatement value) {
      return super.transform(scope.nest(), value);
    }

    /**
     * Resolve accesors and types on {@link AccessExpression}, turning it into Bou
     */
    @Override
    protected Trees.AccessExpression transformAbstract(Scope scope, AccessExpression value) {
      return scope.resolveAccess(value);
    }

    @Override
    protected Expression transformExpression(Scope scope, ApplyExpression value) {
      return simplifyExpression(super.transformExpression(scope, value));
    }

    private Expression simplifyExpression(Expression expression) {
      if (expression instanceof ApplyExpression) {
        ImmutableList<Expression> params = ((ApplyExpression) expression).params();
        if (params.size() == 1) {
          return params.get(0);
        }
      }
      return expression;
    }
  }
}
