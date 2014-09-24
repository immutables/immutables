package org.immutables.modeling.templating;

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
import org.immutables.modeling.Accessors.BoundAccess;
import org.immutables.modeling.SwissArmyKnife;
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
public final class Typing {
  private final SwissArmyKnife knife;

  public Typing(SwissArmyKnife knife) {
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

  @GenerateImmutable(builder = false)
  interface ResolvedType extends Trees.TypeReference, Trees.Synthetic {
    @GenerateConstructorParameter
    TypeMirror typeMirror();
  }

  @GenerateImmutable
  interface ScopeBoundAccess extends Trees.AccessExpression, Trees.Synthetic {
    List<BoundAccess> accessor();
  }

  private class Scope {
    private final Map<String, TypeMirror> locals = Maps.newLinkedHashMap();

    Scope() {}

    Scope(Scope parent) {
      this.locals.putAll(parent.locals);
    }

    Scope chain() {
      return new Scope(this);
    }

    ImmutableTyping.ResolvedType declare(TypeDeclaration type, Trees.Identifier name) {
      return declare(resolve(type), name);
    }

    private ImmutableTyping.ResolvedType declare(TypeMirror type, Trees.Identifier name) {
      locals.put(name.value(), type);
      return ImmutableTyping.ResolvedType.of(type);
    }

    private TypeMirror resolve(TypeDeclaration type) {
      TypeMirror resolved = knife.imports.get(type.type().value());
      if (resolved == null) {
        throw new TypingException(String.format("Could not resolve '%s' simple type", type));
      }
      if (type.kind() == Trees.TypeDeclaration.Kind.ITERABLE) {
        resolved = makeIterableTypeOf(resolved);
      }
      return resolved;
    }

    private DeclaredType makeIterableTypeOf(TypeMirror resolved) {
      return knife.types.getDeclaredType(knife.accessors.iterableElement, resolved);
    }

    ScopeBoundAccess resolveAccess(AccessExpression expression) {
      ImmutableTyping.ScopeBoundAccess.Builder builder =
          ImmutableTyping.ScopeBoundAccess.builder()
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

      if (expression instanceof ScopeBoundAccess) {
        ScopeBoundAccess scopeBoundAccess = (ScopeBoundAccess) expression;
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
    public AssignGenerator transform(Scope context, AssignGenerator value) {
      AssignGenerator generator = super.transform(context, value);
      return generator.withDeclaration(
          context.inferType(
              (ImmutableTrees.ValueDeclaration) generator.declaration(),
              generator.from(),
              Trees.TypeDeclaration.Kind.SCALAR));
    }

    @Override
    public IterationGenerator transform(Scope context, IterationGenerator value) {
      IterationGenerator generator = super.transform(context, value);
      return generator.withDeclaration(
          context.inferType(
              (ImmutableTrees.ValueDeclaration) generator.declaration(),
              generator.from(),
              Trees.TypeDeclaration.Kind.ITERABLE));
    }

    @Override
    public Parameter transform(Scope context, Parameter parameter) {
      return parameter.withType(
          context.declare(
              (TypeDeclaration) parameter.type(),
              parameter.name()));
    }

    @Override
    public Template transform(Scope context, Template value) {
      return super.transform(context.chain(), value);
    }

    @Override
    public LetStatement transform(Scope context, LetStatement value) {
      return super.transform(context.chain(), value);
    }

    @Override
    public ForStatement transform(Scope context, ForStatement value) {
      return super.transform(context.chain(), value);
    }

    @Override
    protected Trees.AccessExpression transformAccessExpression(Scope context, AccessExpression value) {
      return context.resolveAccess(value);
    }
  }
}
