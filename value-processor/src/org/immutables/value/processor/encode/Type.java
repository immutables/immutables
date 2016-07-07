package org.immutables.value.processor.encode;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.immutables.value.processor.encode.Type.Wildcard.Extends;
import org.immutables.value.processor.encode.Type.Wildcard.Super;
import java.util.List;

public interface Type {
	Reference OBJECT = new Reference(Object.class.getName(), true);

	void accept(Visitor visitor);

	interface Visitor {
		void primitive(Primitive primitive);
		void reference(Reference reference);
		void parameterized(Parameterized parameterized);
		void variable(Variable variable);
		void array(Array array);
		void superWildcard(Wildcard.Super wildcard);
		void extendsWildcard(Wildcard.Extends wildcard);
	}

	interface Nonprimitive extends Type {}

	interface Defined extends Nonprimitive {}

	class Reference implements Defined {
		final String name;
		final boolean resolved;

		Reference(String name, boolean resolved) {
			this.name = name;
			this.resolved = resolved;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.reference(this);
		}
	}

	class Array extends Eq<Array> implements Nonprimitive {
		final Type element;

		Array(Type element) {
			super(element);
			this.element = element;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.array(this);
		}

		@Override
		protected boolean eq(Array other) {
			return element.equals(other.element);
		}
	}

	class Variable implements Defined {
		final String name;

		Variable(String name) {
			this.name = name;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.variable(this);
		}
	}

	class Parameterized extends Eq<Parameterized> implements Defined {
		final Reference reference;
		final List<Nonprimitive> arguments;

		Parameterized(Reference reference, List<Nonprimitive> arguments) {
			super(reference, arguments);
			this.reference = reference;
			this.arguments = arguments;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.parameterized(this);
		}

		@Override
		protected boolean eq(Parameterized other) {
			return reference.equals(other.reference)
					&& arguments.equals(other.arguments);
		}
	}

	interface Wildcard extends Nonprimitive {
		class Super extends Eq<Super> implements Wildcard {
			final Defined lowerBound;

			Super(Defined lowerBound) {
				super(lowerBound);
				this.lowerBound = lowerBound;
			}

			@Override
			public void accept(Visitor visitor) {
				visitor.superWildcard(this);
			}

			@Override
			protected boolean eq(Super other) {
				return lowerBound.equals(other.lowerBound);
			}
		}

		class Extends extends Eq<Extends> implements Wildcard {
			final Defined upperBound;

			Extends(Defined upperBound) {
				super(upperBound);
				this.upperBound = upperBound;
			}

			@Override
			public void accept(Visitor visitor) {
				visitor.extendsWildcard(this);
			}

			@Override
			protected boolean eq(Extends other) {
				return upperBound.equals(other.upperBound);
			}
		}
	}

	enum Primitive implements Type {
		BOOLEAN,
		BYTE,
		SHORT,
		INT,
		LONG,
		CHAR,
		FLOAT,
		DOUBLE,
		VOID;

		final String typename;

		Primitive() {
			this.typename = name();
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.primitive(this);
		}

		@Override
		public String toString() {
			return typename;
		}
	}

	interface Factory {
		Type named(String name);

		Parameterized parameterized(Reference raw, Iterable<? extends Nonprimitive> arguments);

		Array array(Type element);

		Wildcard.Super superWildcard(Defined lowerBound);

		Wildcard.Extends extendsWildcard(Defined upperBound);

		Variable variable(String name);

		Reference unresolved(String name);
	}

	class Print implements Visitor {
		private final StringBuilder builder;

		Print() {
			this(new StringBuilder());
		}

		Print(StringBuilder builder) {
			this.builder = builder;
		}

		@Override
		public void primitive(Primitive primitive) {
			builder.append(primitive);
		}

		@Override
		public void reference(Reference reference) {
			builder.append(reference.name);
		}

		@Override
		public void parameterized(Parameterized parameterized) {
			parameterized.reference.accept(this);
			builder.append('<');
			printSeparated(parameterized.arguments, ", ");
			builder.append('>');
		}

		@Override
		public void variable(Variable variable) {
			builder.append(variable.name);
		}

		@Override
		public void array(Array array) {
			array.element.accept(this);
			builder.append("[]");
		}

		@Override
		public void superWildcard(Super wildcard) {
			builder.append("? super ");
			wildcard.lowerBound.accept(this);
		}

		@Override
		public void extendsWildcard(Extends wildcard) {
			if (wildcard.upperBound == OBJECT) {
				builder.append("?");
			} else {
				builder.append("? extends ");
				wildcard.upperBound.accept(this);
			}
		}

		@Override
		public String toString() {
			return builder.toString();
		}

		private void printSeparated(Iterable<? extends Type> types, String separator) {
			boolean notFirst = false;
			for (Type t : types) {
				if (notFirst) {
					builder.append(separator);
				}
				notFirst = true;
				t.accept(this);
			}
		}
	}
}
