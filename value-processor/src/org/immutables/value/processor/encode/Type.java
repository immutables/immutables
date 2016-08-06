package org.immutables.value.processor.encode;

import java.util.Iterator;
import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
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
import javax.annotation.concurrent.ThreadSafe;
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
    public final boolean varargs;

    Array(Type element, boolean varargs) {
      super(element, varargs);
      if (element instanceof Wildcard) {
        throw new IllegalArgumentException("Wildcard as array element is not allowed: " + element);
      }
      this.element = element;
      this.varargs = varargs;
    }

    @Override
    protected boolean eq(Array other) {
      return element.equals(other.element)
          && varargs == other.varargs;
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

    boolean isUnbounded() {
      return upperBounds.isEmpty()
          || upperBounds.equals(ImmutableList.of(OBJECT));
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

    Array varargs(Type element);

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
      return builder.append(array.varargs ? "..." : "[]");
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

  @ThreadSafe
  class Producer implements Type.Factory {
    static final Map<String, Primitive> PRIMITIVE_TYPES;
    static {
      ImmutableMap.Builder<String, Primitive> primitives = ImmutableMap.builder();
      for (Primitive p : Primitive.values()) {
        primitives.put(p.typename, p);
      }
      PRIMITIVE_TYPES = primitives.build();
    }

    private static final Parameters EMPTY_PARAMETERS = new Parameters() {
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

    static Parameters emptyParameters() {
      return EMPTY_PARAMETERS;
    }

    private final Map<String, Reference> resolvedTypes = new HashMap<>(64);
    {
      synchronized (resolvedTypes) {
        resolvedTypes.put(Type.OBJECT.name, Type.OBJECT);
        resolvedTypes.put(Type.STRING.name, Type.STRING);
      }
    }

    @Override
    public Primitive primitive(String name) {
      @Nullable Primitive type = PRIMITIVE_TYPES.get(name);
      Preconditions.checkArgument(type != null, "wrong primitive type name %s", name);
      return type;
    }

    @Override
    public Reference reference(String name) {
      synchronized (resolvedTypes) {
        Reference type = resolvedTypes.get(name);
        if (type == null) {
          type = new Reference(name, true);
          resolvedTypes.put(name, type);
        }
        return type;
      }
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
      return new Array(element, false);
    }

    @Override
    public Array varargs(Type element) {
      return new Array(element, true);
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
      return EMPTY_PARAMETERS;
    }

    static final class DefinedParameters implements Parameters {
      private final @Nullable DefinedParameters parent;
      private final Variable variable;
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
        if (variable.upperBounds.isEmpty()) {
          return variable.name;
        }
        if (variable.upperBounds.size() == 1 && variable.upperBounds.get(0) == Type.OBJECT) {
          return variable.name;
        }
        return variable.name + " extends " + Joiner.on(" & ").join(variable.upperBounds);
      }
    }
  }

  abstract class Transformer implements Visitor<Type> {
    protected Type defaults(Type type) {
      return type;
    }

    @Override
    public Type primitive(Primitive primitive) {
      return defaults(primitive);
    }

    @Override
    public Type reference(Reference reference) {
      return defaults(reference);
    }

    @Override
    public Type variable(Variable variable) {
      return defaults(variable);
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
    public Type array(Array array) {
      return new Array(array.accept(this), array.varargs);
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

  class VariableResolver extends Transformer implements Function<Type, Type> {
    private static final VariableResolver EMPTY = new VariableResolver(new Variable[0], new Nonprimitive[0]);

    public static VariableResolver empty() {
      return EMPTY;
    }

    public boolean isEmpty() {
      return variables.length == 0;
    }

    private final Variable[] variables;
    private final Nonprimitive[] substitutions;

    private VariableResolver(Variable[] variables, Nonprimitive[] substitutions) {
      assert variables.length == substitutions.length;
      this.variables = variables;
      this.substitutions = substitutions;
    }

    public Variable[] variables() {
      return variables.clone();
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

    @Override
    public Type apply(Type type) {
      return type.accept(this);
    }

    @Override
    public String toString() {
      StringBuilder b = new StringBuilder(getClass().getSimpleName()).append("{");
      for (Variable v : variables) {
        b.append("\n  ").append(v).append(" -> ").append(variable(v));
      }
      return b.append("\n}").toString();
    }

    public VariableResolver bind(Variable variable, Nonprimitive substitution) {
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

    Parser unresolving() {
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
        if (!t.isWhitespace())
          terms.add(t);
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
              }
              while (!terms.isEmpty()) {
                t = terms.peek();
                if (t.is("[")) {
                  type = array(type);
                } else {
                  break;
                }
              }
              if (t.is(".")) {
                type = varargs(type);
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
          return factory.array(element);
        }

        Type varargs(Type element) {
          expect(terms.poll(), ".");
          expect(terms.poll(), ".");
          expect(terms.poll(), ".");
          return factory.varargs(element);
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
            } else
              break;
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
              if (terms.size() >= 2) {
                // case when varargs may be so more than one dot
                // but we handle rest of it elsewhere
                Iterator<Term> it = terms.iterator();
                if (it.next().is(".") && it.next().is(".")) {
                  break;
                }
              }
              terms.poll();
            } else {
              break;
            }
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

  // Template matches types, but do not subtyping into account
  // also it doesn't verify variable bounds
  class Template {
    public final Type template;

    public Template(Type template) {
      this.template = template;
    }

    public Optional<VariableResolver> match(Type type) {
      final VariableResolver[] holder = new VariableResolver[] {VariableResolver.empty()};

      class Decomposer implements Visitor<Boolean> {
        final Type type;

        Decomposer(Type type) {
          this.type = type;
        }

        @Override
        public Boolean primitive(Primitive primitive) {
          return type == primitive;
        }

        @Override
        public Boolean reference(Reference reference) {
          return type.equals(reference);
        }

        @Override
        public Boolean parameterized(Parameterized template) {
          if (type instanceof Parameterized) {
            Parameterized ps = (Parameterized) type;
            if (template.reference.equals(ps.reference)
                && ps.arguments.size() == template.arguments.size()) {
              for (int i = 0; i < template.arguments.size(); i++) {
                Decomposer visitor = new Decomposer(ps.arguments.get(i));
                boolean matched = template.arguments.get(i).accept(visitor);
                if (!matched) {
                  return false;
                }
              }
              return true;
            }
          }
          return false;
        }

        @Override
        public Boolean variable(Variable variable) {
          if (variable.equals(type)) {
            return true;
          }
          if (type instanceof Nonprimitive) {
            holder[0] = holder[0].bind(variable, (Nonprimitive) type);
            return true;
          }
          return false;
        }

        @Override
        public Boolean array(Array array) {
          if (type instanceof Array) {
            return array.element.accept(new Decomposer(((Array) type).element));
          }
          return false;
        }

        @Override
        public Boolean superWildcard(Wildcard.Super wildcard) {
          if (type instanceof Wildcard.Super) {
            return wildcard.lowerBound.accept(new Decomposer(((Wildcard.Super) type).lowerBound));
          }
          return false;
        }

        @Override
        public Boolean extendsWildcard(Wildcard.Extends wildcard) {
          if (type instanceof Wildcard.Extends) {
            return wildcard.upperBound.accept(new Decomposer(((Wildcard.Extends) type).upperBound));
          }
          return false;
        }
      }

      if (template.accept(new Decomposer(type))) {
        return Optional.of(holder[0]);
      }

      return Optional.absent();
    }
  }
}
