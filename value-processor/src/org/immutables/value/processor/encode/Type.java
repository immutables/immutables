package org.immutables.value.processor.encode;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.processor.encode.Code.Term;

/**
 * I've created this type model as an experiment which I want to bring forward and evolve
 * into something more general purpose, probably, an utility library some day.
 * The idea here is to give practical, yet accurate depiction of the type system in java using
 * immutable types, being lightweight, efficient and easy to analyse and transform. Apparently,
 * neither {@code java.lang.reflect} nor {@code javax.lang.model} (nor any 3rd party lib I've seen)
 * are not suitable for anything I want to do.
 * <p>
 * What we tried to avoid:
 * <ul>
 * <li>Compicated reverse references and mutability</li>
 * <li>Idealism and unrealistic overgeneralization ({@link java.lang.reflect.WildcardType} anyone?)</li>
 * <li>Complicated types like intersection or unions where we get without them</li>
 * </ul>
 * Moreover we are only concerned with the types in signatures, not a whole spectre of types which
 * might occur in java code.
 */
public interface Type {
	Reference OBJECT = new Reference(Object.class.getName(), true);
	Reference STRING = new Reference(String.class.getName(), true);

	<V> V accept(Visitor<V> visitor);

	interface Visitor<V> {
		V primitive(Primitive primitive);
		V reference(Reference reference);
		V parameterized(Parameterized parameterized);
		V variable(Variable variable);
		V array(Array array);
		V superWildcard(Wildcard.Super wildcard);
		V extendsWildcard(Wildcard.Extends wildcard);
	}

	interface Nonprimitive extends Type {}

	interface Defined extends Nonprimitive {}

	class Reference implements Defined {
		public final String name;
		public final boolean resolved;

		Reference(String name, boolean resolved) {
			this.name = name;
			this.resolved = resolved;
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {
			return visitor.reference(this);
		}

		@Override
		public String toString() {
			return accept(new Print()).toString();
		}
	}

	class Array extends Eq<Array> implements Nonprimitive {
		public final Type element;

		Array(Type element) {
			super(element);
			if (element instanceof Wildcard) {
				throw new IllegalArgumentException("Wildcard as array element is not allowed: " + element);
			}
			this.element = element;
		}

		@Override
		protected boolean eq(Array other) {
			return element.equals(other.element);
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {
			return visitor.array(this);
		}

		@Override
		public String toString() {
			return accept(new Print()).toString();
		}
	}

	class Variable implements Defined {
		public final String name;
		public final List<Defined> upperBounds;

		Variable(String name, List<Defined> upperBounds) {
			this.name = name;
			this.upperBounds = upperBounds;
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {
			return visitor.variable(this);
		}

		@Override
		public String toString() {
			return accept(new Print()).toString();
		}
	}

	class Parameterized extends Eq<Parameterized> implements Defined {
		public final Reference reference;
		public final List<Nonprimitive> arguments;

		Parameterized(Reference reference, List<Nonprimitive> arguments) {
			super(reference, arguments);
			this.reference = reference;
			this.arguments = arguments;
		}

		@Override
		protected boolean eq(Parameterized other) {
			return reference.equals(other.reference)
					&& arguments.equals(other.arguments);
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {
			return visitor.parameterized(this);
		}

		@Override
		public String toString() {
			return accept(new Print()).toString();
		}
	}

	interface Wildcard extends Nonprimitive {
		class Super extends Eq<Super> implements Wildcard {
			public final Defined lowerBound;

			Super(Defined lowerBound) {
				super(lowerBound);
				this.lowerBound = lowerBound;
			}

			@Override
			protected boolean eq(Super other) {
				return lowerBound.equals(other.lowerBound);
			}
			@Override
			public <V> V accept(Visitor<V> visitor) {
				return visitor.superWildcard(this);
			}

			@Override
			public String toString() {
				return accept(new Print()).toString();
			}
		}

		class Extends extends Eq<Extends> implements Wildcard {
			public final Defined upperBound;

			Extends(Defined upperBound) {
				super(upperBound);
				this.upperBound = upperBound;
			}

			@Override
			protected boolean eq(Extends other) {
				return upperBound.equals(other.upperBound);
			}

			@Override
			public <V> V accept(Visitor<V> visitor) {
				return visitor.extendsWildcard(this);
			}

			@Override
			public String toString() {
				return accept(new Print()).toString();
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
			this.typename = Ascii.toLowerCase(name());
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {
			return visitor.primitive(this);
		}

		@Override
		public String toString() {
			return typename;
		}
	}

	interface Parameters {
		Parameters introduce(String name, Iterable<? extends Defined> upperBounds);
		List<String> names();
		Variable variable(String name);
	}

	interface Factory {
		Primitive primitive(String name);
		Reference reference(String name);
		Reference unresolved(String name);
		Parameterized parameterized(Reference raw, Iterable<? extends Nonprimitive> arguments);
		Array array(Type element);
		Wildcard.Super superWildcard(Defined lowerBound);
		Wildcard.Extends extendsWildcard(Defined upperBound);
		Parameters parameters();
	}

	class Print implements Visitor<StringBuilder> {
		private final StringBuilder builder;

		Print() {
			this(new StringBuilder());
		}

		Print(StringBuilder builder) {
			this.builder = builder;
		}

		@Override
		public StringBuilder primitive(Primitive primitive) {
			return builder.append(primitive);
		}

		@Override
		public StringBuilder reference(Reference reference) {
			return builder.append(reference.name);
		}

		@Override
		public StringBuilder parameterized(Parameterized parameterized) {
			parameterized.reference.accept(this);
			builder.append('<');
			printSeparated(parameterized.arguments, ", ");
			return builder.append('>');
		}

		@Override
		public StringBuilder variable(Variable variable) {
			return builder.append(variable.name);
		}

		@Override
		public StringBuilder array(Array array) {
			array.element.accept(this);
			return builder.append("[]");
		}

		@Override
		public StringBuilder superWildcard(Wildcard.Super wildcard) {
			builder.append("? super ");
			return wildcard.lowerBound.accept(this);
		}

		@Override
		public StringBuilder extendsWildcard(Wildcard.Extends wildcard) {
			if (wildcard.upperBound == OBJECT) {
				return builder.append("?");
			}
			builder.append("? extends ");
			return wildcard.upperBound.accept(this);
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

	class Producer implements Type.Factory {
		static final Map<String, Primitive> PRIMITIVE_TYPES;
		static {
			ImmutableMap.Builder<String, Primitive> primitives = ImmutableMap.builder();
			for (Primitive p : Primitive.values()) {
				primitives.put(p.typename, p);
			}
			PRIMITIVE_TYPES = primitives.build();
		}

		private static final Parameters INITIAL_PARAMETERS = new Parameters() {
			@Override
			public Variable variable(String name) {
				throw new IllegalArgumentException("no such type parameter '" + name + "' defined in " + this);
			}
			@Override
			public Parameters introduce(String name, Iterable<? extends Defined> upperBounds) {
				return new DefinedParameters(null, name, ImmutableList.copyOf(upperBounds));
			}
			@Override
			public List<String> names() {
				return ImmutableList.of();
			}
			@Override
			public String toString() {
				return "";
			}
		};

		private final Map<String, Reference> resolvedTypes = new HashMap<>(32);
		{
			resolvedTypes.put(Type.OBJECT.name, Type.OBJECT);
			resolvedTypes.put(Type.STRING.name, Type.STRING);
		}

		@Override
		public Primitive primitive(String name) {
			@Nullable Primitive type = PRIMITIVE_TYPES.get(name);
			Preconditions.checkArgument(type != null, "wrong primitive type name %s", name);
			return type;
		}

		@Override
		public Reference reference(String name) {
			Reference type = resolvedTypes.get(name);
			if (type == null) {
				type = new Reference(name, true);
				resolvedTypes.put(name, type);
			}
			return type;
		}

		@Override
		public Reference unresolved(String name) {
			return new Reference(name, false);
		}

		@Override
		public Parameterized parameterized(Reference raw, Iterable<? extends Nonprimitive> arguments) {
			return new Parameterized(raw, ImmutableList.copyOf(arguments));
		}

		@Override
		public Array array(Type element) {
			return new Array(element);
		}

		@Override
		public Wildcard.Super superWildcard(Defined lowerBound) {
			return new Wildcard.Super(lowerBound);
		}

		@Override
		public Wildcard.Extends extendsWildcard(Defined upperBound) {
			return new Wildcard.Extends(upperBound);
		}

		@Override
		public Parameters parameters() {
			return INITIAL_PARAMETERS;
		}

		static final class DefinedParameters implements Parameters {
			final @Nullable DefinedParameters parent;
			final Variable variable;
			private final List<String> names;

			DefinedParameters(@Nullable DefinedParameters parent, String name, List<Defined> bounds) {
				this.parent = parent;
				this.variable = new Variable(name, bounds);
				this.names = unwindNames();
			}

			private ImmutableList<String> unwindNames() {
				ImmutableList.Builder<String> builder = ImmutableList.builder();
				for (DefinedParameters p = this; p != null; p = p.parent) {
					builder.add(p.variable.name);
				}
				return builder.build().reverse();
			}

			@Override
			public Parameters introduce(String name, Iterable<? extends Defined> upperBounds) {
				return new DefinedParameters(this, name, ImmutableList.copyOf(upperBounds));
			}

			@Override
			public Variable variable(String name) {
				if (variable.name.equals(name)) {
					return variable;
				}
				if (parent != null) {
					return parent.variable(name);
				}
				throw new IllegalArgumentException("no such variable: " + name);
			}

			@Override
			public List<String> names() {
				return names;
			}

			@Override
			public String toString() {
				String parameters = "";
				for (DefinedParameters p = this; p != null; p = p.parent) {
					parameters = !parameters.isEmpty()
							? p.parameterString() + ", " + parameters
							: p.parameterString();
				}
				return "<" + parameters + ">";
			}

			private String parameterString() {
				if (!variable.upperBounds.isEmpty()) {
					return variable.name + " extends " + Joiner.on(" & ").join(variable.upperBounds);
				}
				return variable.name;
			}
		}
	}

	abstract class Transformer implements Visitor<Type> {

		@Override
		public Type primitive(Primitive primitive) {
			return primitive;
		}

		@Override
		public Type reference(Reference reference) {
			return reference;
		}

		@Override
		public Type parameterized(Parameterized parameterized) {
			ImmutableList.Builder<Nonprimitive> builder = ImmutableList.builder();
			for (Nonprimitive a : parameterized.arguments) {
				builder.add((Nonprimitive) a.accept(this));
			}
			return new Parameterized(
					(Reference) parameterized.reference.accept(this),
					builder.build());
		}

		@Override
		public Type variable(Variable variable) {
			return variable;
		}

		@Override
		public Type array(Array array) {
			return new Array(array.accept(this));
		}

		@Override
		public Type superWildcard(Wildcard.Super wildcard) {
			return new Wildcard.Super((Defined) wildcard.lowerBound.accept(this));
		}

		@Override
		public Type extendsWildcard(Wildcard.Extends wildcard) {
			return new Wildcard.Extends((Defined) wildcard.upperBound.accept(this));
		}
	}

	class VariableResolver extends Transformer {
		private static final VariableResolver START = new VariableResolver(new Variable[0], new Nonprimitive[0]);

		public static VariableResolver start() {
			return START;
		}

		private final Variable[] variables;
		private final Nonprimitive[] substitutions;

		private VariableResolver(Variable[] variables, Nonprimitive[] substitutions) {
			this.variables = variables;
			this.substitutions = substitutions;
		}

		@Override
		public Type variable(Variable variable) {
			for (int i = 0; i < variables.length; i++) {
				if (variables[i] == variable) {
					return substitutions[i];
				}
			}
			return variable;
		}

		public VariableResolver resolve(Variable variable, Nonprimitive substitution) {
			Variable[] variables = Arrays.copyOf(this.variables, this.variables.length + 1);
			variables[variables.length - 1] = variable;

			Nonprimitive[] substitutions = Arrays.copyOf(this.substitutions, this.substitutions.length + 1);
			substitutions[substitutions.length - 1] = substitution;

			return new VariableResolver(variables, substitutions);
		}
	}

	class Parser {
		private static final Joiner JOINER = Joiner.on('.');

		private boolean unresolve;
		private final Factory factory;
		private final Parameters parameters;

		public Parser(Factory factory, Parameters parameters) {
			this.factory = factory;
			this.parameters = parameters;
		}

		Parser unresolved() {
			unresolve = false;
			return this;
		}

		private Type forName(String name) {
			if (Producer.PRIMITIVE_TYPES.containsKey(name)) {
				return factory.primitive(name);
			}
			if (parameters.names().contains(name)) {
				return parameters.variable(name);
			}
			return unresolve
					? factory.unresolved(name)
					: factory.reference(name);
		}

		Type parse(String input) {
			try {
				return doParse(input);
			} catch (RuntimeException ex) {
				IllegalArgumentException exception =
						new IllegalArgumentException(
								"Cannot parse type from input string '" + input + "'. " + ex.getMessage());
				exception.setStackTrace(ex.getStackTrace());
				throw exception;
			}
		}

		private Type doParse(String input) {
			final Deque<Term> terms = new LinkedList<>();

			for (Term t : Code.termsFrom(input)) {
				if (!t.isWhitespace()) terms.add(t);
			}

			class Reader {

				Type type() {
					Term t = terms.peek();
					if (t.isWordOrNumber()) {
						Type type = named();

						t = terms.peek();
						if (t != null) {
							if (t.is("<")) {
								type = arguments(type);
							} else if (t.is("[")) {
								type = array(type);
							}
						}
						return type;
					}
					throw new IllegalStateException("unexpected term '" + t + "'");
				}

				Wildcard wildcard() {
					expect(terms.poll(), "?");

					Term t = terms.peek();
					if (t.is("super")) {
						terms.poll();
						Defined lowerBound = (Defined) type();
						return factory.superWildcard(lowerBound);
					}
					if (t.is("extends")) {
						terms.poll();
						Defined upperBound = (Defined) type();
						return factory.extendsWildcard(upperBound);
					}
					return factory.extendsWildcard(OBJECT);
				}

				Type array(Type element) {
					expect(terms.poll(), "[");
					expect(terms.poll(), "]");

					Type type = factory.array(element);

					if (!terms.isEmpty() && terms.peek().is("[")) {
						return array(type);
					}

					return type;
				}

				Type arguments(Type reference) {
					expect(terms.poll(), "<");

					List<Nonprimitive> arguments = new ArrayList<>();
					for (;;) {
						if (terms.peek().is("?")) {
							arguments.add(wildcard());
						} else {
							arguments.add((Nonprimitive) type());
						}
						if (!terms.isEmpty() && terms.peek().is(",")) {
							terms.poll();
						} else break;
					}

					expect(terms.poll(), ">");

					return factory.parameterized((Reference) reference, arguments);
				}

				Type named() {
					List<String> segments = new ArrayList<>();

					for (;;) {
						Term t = terms.poll();
						assert t.isWordOrNumber();
						segments.add(t.toString());

						if (!terms.isEmpty() && terms.peek().is(".")) {
							terms.poll();
						} else break;
					}

					return forName(JOINER.join(segments));
				}

				void expect(Term t, String is) {
					if (t == null || !t.is(is)) {
						throw new IllegalStateException("expected '" + is + "' but got '" + t + "'");
					}
				}
			}

			Type type = new Reader().type();

			if (!terms.isEmpty()) {
				throw new IllegalStateException("unexpected trailing terms '" + Joiner.on("").join(terms) + "'");
			}

			return type;
		}
	}
}
