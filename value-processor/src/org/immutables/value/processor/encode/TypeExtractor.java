package org.immutables.value.processor.encode;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
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
import org.immutables.value.processor.encode.Type.Defined;
import org.immutables.value.processor.encode.Type.Factory;
import org.immutables.value.processor.encode.Type.Nonprimitive;
import org.immutables.value.processor.encode.Type.Parameters;
import org.immutables.value.processor.encode.Type.Reference;

final class TypeExtractor {
	private final Factory factory;
	private final Parameters parameters;
	private final TypeConverter converter = new TypeConverter();

	TypeExtractor(Type.Factory factory, TypeElement context) {
		this.factory = factory;
		this.parameters = initParameters(context);
	}

	private Parameters initParameters(TypeElement context) {
		Parameters parameters = factory.parameters();

		for (TypeParameterElement p : context.getTypeParameters()) {
			String name = p.getSimpleName().toString();
			List<Defined> bounds = getBounds(parameters, p);

			parameters = parameters.introduce(name, bounds);
		}

		return parameters;
	}

	private List<Defined> getBounds(Parameters parameters, TypeParameterElement p) {
		List<Defined> bounds = new ArrayList<>();
		for (TypeMirror b : p.getBounds()) {
			bounds.add((Defined) b.accept(converter, parameters));
		}
		return bounds;
	}

	Type get(TypeMirror type) {
		return type.accept(converter, parameters);
	}

	class TypeConverter extends AbstractTypeVisitor7<Type, Parameters> {

		@Override
		public Type visitPrimitive(PrimitiveType t, Parameters p) {
			return factory.primitive(t.toString());
		}

		@Override
		public Type visitArray(ArrayType t, Parameters p) {
			return factory.array(t.getComponentType().accept(this, p));
		}

		@Override
		public Type visitDeclared(DeclaredType t, Parameters p) {
			Reference reference = factory.reference(qualifiedNameOf(t));
			List<? extends TypeMirror> typeArguments = t.getTypeArguments();
			if (typeArguments.isEmpty()) {
				return reference;
			}
			List<Nonprimitive> args = new ArrayList<>();
			for (TypeMirror a : typeArguments) {
				args.add((Nonprimitive) a.accept(this, p));
			}
			return factory.parameterized(reference, args);
		}

		private String qualifiedNameOf(DeclaredType t) {
			return ((TypeElement) t.asElement()).getQualifiedName().toString();
		}

		@Override
		public Type visitTypeVariable(TypeVariable t, Parameters p) {
			String v = t.asElement().getSimpleName().toString();
			return p.variable(v);
		}

		@Override
		public Type visitWildcard(WildcardType t, Parameters p) {
			@Nullable TypeMirror superBound = t.getSuperBound();
			if (superBound != null) {
				return factory.superWildcard((Defined) superBound.accept(this, p));
			}
			@Nullable TypeMirror extendsBound = t.getExtendsBound();
			if (extendsBound != null) {
				return factory.extendsWildcard((Defined) extendsBound.accept(this, p));
			}
			return factory.extendsWildcard(Type.OBJECT);
		}

		@Override
		public Type visitError(ErrorType t, Parameters p) {
			throw new UnsupportedOperationException("ErrorType type not supported");
		}

		@Override
		public Type visitExecutable(ExecutableType t, Parameters p) {
			throw new UnsupportedOperationException("ExecutableType type not supported");
		}

		@Override
		public Type visitNoType(NoType t, Parameters p) {
			throw new UnsupportedOperationException("NoType type not supported");
		}

		@Override
		public Type visitUnion(UnionType t, Parameters p) {
			throw new UnsupportedOperationException("UnionType type not supported");
		}

		@Override
		public Type visitNull(NullType t, Parameters p) {
			throw new UnsupportedOperationException("NullType type not supported");
		}
	}
}
