package org.immutables.value.processor.encode;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor7;

final class TypeExtractor {
  final Type.Factory factory;
  final Type.Parameters parameters;
  final Type.Parser parser;
  private final TypeConverter converter = new TypeConverter();

  TypeExtractor(Type.Factory factory, Parameterizable context) {
    this.factory = factory;
    this.parameters = initParameters(context);
    this.parser = new Type.Parser(factory, parameters);
  }

  TypeExtractor(Type.Factory factory, Type.Parameters parameters) {
    this.factory = factory;
    this.parameters = parameters;
    this.parser = new Type.Parser(factory, parameters);
  }

  TypeExtractor withParameter(String name, Iterable<? extends Type.Defined> bounds) {
    return new TypeExtractor(
        factory,
        parameters.introduce(name, bounds));
  }

  ImmutableList<Type.Defined> getDefined(Iterable<? extends TypeMirror> bounds) {
    ImmutableList.Builder<Type.Defined> builder = ImmutableList.builder();
    for (TypeMirror b : bounds) {
      builder.add((Type.Defined) get(b));
    }
    return builder.build();
  }

  private Type.Parameters initParameters(Parameterizable context) {
    Type.Parameters parameters = factory.parameters();

    for (TypeParameterElement p : context.getTypeParameters()) {
      String name = p.getSimpleName().toString();
      List<Type.Defined> bounds = getBounds(parameters, p);

      parameters = parameters.introduce(name, bounds);
    }

    return parameters;
  }

  private List<Type.Defined> getBounds(Type.Parameters parameters, TypeParameterElement p) {
    List<Type.Defined> bounds = new ArrayList<>();
    for (TypeMirror b : p.getBounds()) {
      bounds.add((Type.Defined) b.accept(converter, parameters));
    }
    return bounds;
  }

  Type get(TypeMirror type) {
    return type.accept(converter, parameters);
  }

  class TypeConverter extends AbstractTypeVisitor7<Type, Type.Parameters> {

    @Override
    public Type visitPrimitive(PrimitiveType t, Type.Parameters p) {
      return factory.primitive(t.toString());
    }

    @Override
    public Type visitArray(ArrayType t, Type.Parameters p) {
      return factory.array(t.getComponentType().accept(this, p));
    }

    @Override
    public Type visitDeclared(DeclaredType t, Type.Parameters p) {
      Type.Reference reference = factory.reference(qualifiedNameOf(t));
      List<? extends TypeMirror> typeArguments = t.getTypeArguments();
      if (typeArguments.isEmpty()) {
        return reference;
      }
      List<Type.Nonprimitive> args = new ArrayList<>();
      for (TypeMirror a : typeArguments) {
        args.add((Type.Nonprimitive) a.accept(this, p));
      }
      return factory.parameterized(reference, args);
    }

    private String qualifiedNameOf(DeclaredType t) {
      return ((TypeElement) t.asElement()).getQualifiedName().toString();
    }

    @Override
    public Type visitTypeVariable(TypeVariable t, Type.Parameters p) {
      String v = t.asElement().getSimpleName().toString();
      return p.variable(v);
    }

    @Override
    public Type visitWildcard(WildcardType t, Type.Parameters p) {
      @Nullable TypeMirror superBound = t.getSuperBound();
      if (superBound != null) {
        return factory.superWildcard((Type.Defined) superBound.accept(this, p));
      }
      @Nullable TypeMirror extendsBound = t.getExtendsBound();
      if (extendsBound != null) {
        return factory.extendsWildcard((Type.Defined) extendsBound.accept(this, p));
      }
      return factory.extendsWildcard(Type.OBJECT);
    }

    @Override
    public Type visitNoType(NoType t, Type.Parameters p) {
      return Type.Primitive.VOID;
    }

    @Override
    public Type visitError(ErrorType t, Type.Parameters p) {
      throw new UnsupportedOperationException("ErrorType type not supported");
    }

    @Override
    public Type visitExecutable(ExecutableType t, Type.Parameters p) {
      throw new UnsupportedOperationException("ExecutableType type not supported");
    }

    @Override
    public Type visitUnion(UnionType t, Type.Parameters p) {
      throw new UnsupportedOperationException("UnionType type not supported");
    }

    @Override
    public Type visitNull(NullType t, Type.Parameters p) {
      throw new UnsupportedOperationException("NullType type not supported");
    }
  }
}
