package org.immutables.modeling.templating;

import org.immutables.modeling.templating.Trees.TypeDeclaration.Kind;
import com.google.common.base.Preconditions;
import org.immutables.modeling.templating.Trees.TypeIdentifier;
import org.immutables.modeling.templating.Trees.TypeReference;
import org.immutables.modeling.processing.SwissArmyKnife;
import org.immutables.modeling.processing.Accessors.BoundAccess;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.immutables.modeling.templating.ImmutableTrees.AccessExpression;
import org.immutables.modeling.templating.ImmutableTrees.ApplyExpression;
import org.immutables.modeling.templating.ImmutableTrees.AssignGenerator;
import org.immutables.modeling.templating.ImmutableTrees.BoundAccessExpression;
import org.immutables.modeling.templating.ImmutableTrees.ForIterationAccessExpression;
import org.immutables.modeling.templating.ImmutableTrees.ForStatement;
import org.immutables.modeling.templating.ImmutableTrees.Identifier;
import org.immutables.modeling.templating.ImmutableTrees.IterationGenerator;
import org.immutables.modeling.templating.ImmutableTrees.LetStatement;
import org.immutables.modeling.templating.ImmutableTrees.Parameter;
import org.immutables.modeling.templating.ImmutableTrees.ResolvedType;
import org.immutables.modeling.templating.ImmutableTrees.Template;
import org.immutables.modeling.templating.ImmutableTrees.TypeDeclaration;
import org.immutables.modeling.templating.ImmutableTrees.Unit;
import org.immutables.modeling.templating.Trees.Expression;
import org.immutables.modeling.templating.Trees.TemplatePart;
import static com.google.common.base.Preconditions.*;

public final class Resolver {
  static final String ITERATION_ACCESS_VARIABLE = "for";

  private final SwissArmyKnife knife;

  public Resolver(SwissArmyKnife knife) {
    this.knife = knife;
  }

  public static class TypingException extends RuntimeException {
    TypingException(String message) {
      super(message);
    }
  }

  public Unit resolve(Unit unit) {
    return new Transformer()
        .transform(new Scope(),
            new ForIterationAccessTransformer()
                .transform((Void) null, unit));
  }

  private class Scope {
    final Map<String, TypeMirror> locals = Maps.newLinkedHashMap();

    Scope nest() {
      Scope nested = new Scope();
      nested.locals.putAll(locals);
      return nested;
    }

    ResolvedType declare(TypeDeclaration type, Trees.Identifier name) {
      if (isDeclared(name)) {
        throw new TypingException(String.format("Redeclaration of local %s", name));
      }
      return declare(resolve(type), name);
    }

    /**
     * Declare template or invokable. There's no {{@code isDeclared} check because we potentially
     * might allow to define several templates with the same name but different types of arguments,
     * to be resolved at runtime. (akin to multimethods). Might need to check if the same
     * combination of parameters was already used.
     * @param name identifier
     * @return resolved type
     */
    ResolvedType declareInvokable(Trees.Identifier name) {
      return declare(knife.accessors.invokableType, name);
    }

    ResolvedType declareForIterationAccess(Trees.Identifier name) {
      return declare(knife.accessors.iterationType, name);
    }

    boolean isDeclared(Trees.Identifier name) {
      return locals.containsKey(name.value());
    }

    ResolvedType declare(TypeMirror type, Trees.Identifier name) {
      locals.put(name.value(), type);
      return ResolvedType.of(type);
    }

    TypeMirror resolve(TypeDeclaration type) {
      TypeMirror resolved = knife.imports.get(type.type().value());
      if (resolved == null) {
        throw new TypingException(String.format("Could not resolve %s simple type", type));
      }
      if (type.kind() == Trees.TypeDeclaration.Kind.ITERABLE) {
        resolved = makeIterableTypeOf(resolved);
      }
      return resolved;
    }

    DeclaredType makeIterableTypeOf(TypeMirror resolved) {
      return knife.types.getDeclaredType(knife.accessors.iterableElement, resolved);
    }

    BoundAccessExpression resolveAccess(AccessExpression expression) {
      BoundAccessExpression.Builder builder =
          BoundAccessExpression.builder()
              .addAllPath(expression.path());

      BoundAccess accessor = null;
      for (Trees.Identifier identifier : expression.path()) {
        accessor = bindAccess(accessor, identifier.value());
        builder.addAccessor(accessor);
      }

      return builder.build();
    }

    BoundAccess bindAccess(@Nullable BoundAccess previous, String name) {
      return previous != null
          ? knife.binder.bind(previous.type, name)
          : knife.binder.bindLocalOrThis(knife.type.asType(), name, locals);
    }

    Trees.ValueDeclaration inferType(
        ImmutableTrees.ValueDeclaration declaration,
        Trees.Expression expression,
        Trees.TypeDeclaration.Kind kind) {

      if (expression instanceof BoundAccessExpression) {
        BoundAccessExpression scopeBoundAccess = (BoundAccessExpression) expression;
        BoundAccess lastAccess = Iterables.getLast(asBoundAccess(scopeBoundAccess.accessor()));

        if (kind == Trees.TypeDeclaration.Kind.ITERABLE) {
          if (!lastAccess.isContainer()) {
            throw new TypingException(String.format("Not iterable type '%s'%n\tin expression '%s'",
                lastAccess.type,
                scopeBoundAccess.path()));
          }
        }

        if (declaration.type().isPresent()) {
          return declaration.withType(resolveDeclared(declaration.type().get(), declaration.name()));
        }

        TypeMirror resolved = (kind == Trees.TypeDeclaration.Kind.ITERABLE)
            ? lastAccess.containedType
            : lastAccess.type;

        return declaration.withType(declare(resolved, declaration.name()));
      }

      if (declaration.type().isPresent()) {
        return declaration.withType(resolveDeclared(declaration.type().get(), declaration.name()));
      }

      throw new TypingException(String.format("Value should be typed '%s'%n\texpression '%s'",
          declaration.name(),
          expression));
    }

    private ResolvedType resolveDeclared(TypeReference typeReference, Trees.Identifier name) {
      // TBD check type here if it's present
      Preconditions.checkState(typeReference instanceof TypeDeclaration);
      TypeDeclaration typeDeclaration = (TypeDeclaration) typeReference;
      TypeIdentifier type = typeDeclaration.type();
      @Nullable
      TypeMirror resolved = knife.imports.get(type.value());
      if (resolved == null) {
        throw new TypingException(String.format("Could not resolve declared type '%s'",
            typeDeclaration));
      }
      if (typeDeclaration.kind() == Kind.ITERABLE) {
        resolved = knife.accessors.wrapIterable(resolved);
      }
      return declare(resolved, name);
    }
  }

  private static final class ForIterationAccessTransformer extends TreesTransformer<Void> {
    @Override
    protected Expression transformExpression(Void context, ForIterationAccessExpression expression) {
      return AccessExpression.builder()
          .addPath(Identifier.of(ITERATION_ACCESS_VARIABLE))
          .addAllPath(expression.access().path())
          .build();
    }
  }

  private static final class Transformer extends TreesTransformer<Scope> {

    @Override
    public Unit transform(Scope scope, Unit unit) {
      for (Template template : Iterables.filter(unit.parts(), Template.class)) {
        scope.declareInvokable(template.declaration().name());
      }
      return super.transform(scope, unit);
    }

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
    protected TemplatePart transformTemplatePart(Scope scope, LetStatement statement) {
      scope.declareInvokable(statement.declaration().name());
      return super.transformTemplatePart(scope, statement);
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

    /** We prevent transformation here to manually do it after variable declaration is done. */
    @Override
    protected Expression transformIterationGeneratorConditionElement(
        Scope scope,
        IterationGenerator value,
        Expression element) {
      return element;
    }

    @Override
    public Parameter transform(Scope scope, Parameter parameter) {
      return parameter.withType(
          scope.declare(
              (TypeDeclaration) parameter.type(),
              parameter.name()));
    }

    /** Overriden to specify order in which we process declaration first, and then parts. */
    @Override
    public Template transform(Scope scope, Template template) {
      scope = scope.nest();
      return template
          .withDeclaration(transformTemplateDeclaration(scope, template, template.declaration()))
          .withParts(transformTemplateParts(scope, template, template.parts()));
    }

    /** Overriden to specify order in which we process declaration first, and then parts. */
    @Override
    public LetStatement transform(Scope scope, LetStatement statement) {
      scope = scope.nest();
      return statement
          .withDeclaration(transformLetStatementDeclaration(scope, statement, statement.declaration()))
          .withParts(transformLetStatementParts(scope, statement, statement.parts()));
    }

    /** Overriden to specify order in which we process declaration first, and then parts. */
    @Override
    public ForStatement transform(Scope scope, ForStatement statement) {
      scope = scope.nest();
      scope.declareForIterationAccess(Identifier.of(ITERATION_ACCESS_VARIABLE));
      return statement
          .withDeclaration(transformForStatementDeclaration(scope, statement, statement.declaration()))
          .withParts(transformForStatementParts(scope, statement, statement.parts()));
    }

    @Override
    protected Iterable<TemplatePart> transformForStatementParts(
        Scope context,
        ForStatement value,
        List<TemplatePart> collection) {
      return super.transformForStatementParts(context, value, collection);
    }

    /**
     * Resolve accesors and types on {@link AccessExpression}, turning it into
     * {@link BoundAccessExpression}
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

  public static ImmutableList<BoundAccess> asBoundAccess(Iterable<?> iterable) {
    for (Object object : iterable) {
      checkArgument(object instanceof BoundAccess);
    }
    return FluentIterable.from(iterable)
        .filter(BoundAccess.class)
        .toList();
  }
}
