package org.immutables.value.processor.encode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.immutables.value.processor.encode.Type.Defined;
import org.immutables.value.processor.encode.Type.VariableResolver;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class TypeTest {
  private static final ImmutableList<Defined> NO_BOUNDS = ImmutableList.<Defined>of();

  Type.Producer factory = new Type.Producer();

  Type.Parameters parameters = factory.parameters()
      .introduce("A", NO_BOUNDS)
      .introduce("B", NO_BOUNDS)
      .introduce("D", NO_BOUNDS);

  Type.Parser parser = new Type.Parser(factory, parameters);

  @Test
  public void templateMatch() {
    Type template = parser.parse("java.lang.Iterable<A>");
    Type specific = parser.parse("java.lang.Iterable<java.lang.String>");

    Optional<VariableResolver> match = new Type.Template(template).match(specific);
    check(match.isPresent());
    check(template.accept(match.get())).is(specific);

    check(!new Type.Template(specific).match(template).isPresent());
  }

  @Test
  public void templateMoreMatches() {
    check(!new Type.Template(Type.STRING).match(Type.OBJECT).isPresent());
    check(new Type.Template(parser.parse("A")).match(Type.OBJECT).isPresent());
    check(new Type.Template(parser.parse("B")).match(Type.STRING).isPresent());

    VariableResolver nestedMatch =
        new Type.Template(parser.parse("Map<A, List<B>>")).match(parser.parse("Map<java.lang.String, List<D>>")).get();

    check(nestedMatch.variable(parameters.variable("A"))).is(Type.STRING);
    check(nestedMatch.variable(parameters.variable("B"))).is(parameters.variable("D"));
  }

  @Test
  public void resolveVariable() {
    Type map = parser.parse("java.util.Map<A, B>");

    Type.VariableResolver resolver = Type.VariableResolver.empty()
        .resolve(parameters.variable("A"), Type.OBJECT)
        .resolve(parameters.variable("B"), parameters.introduce("C", NO_BOUNDS).variable("C"));

    Type substituted = map.accept(resolver);

    check(substituted).hasToString("java.util.Map<java.lang.Object, C>");
  }

  @Test
  public void parseBasics() {
    check(parser.parse("boolean")).is(Type.Primitive.BOOLEAN);
    check(parser.parse("void")).is(Type.Primitive.VOID);
    check(parser.parse("java.lang.Object")).same(Type.OBJECT);
    check(parser.parse("java.util.List")).same(parser.parse("java.util.List"));
    check(parser.parse("boolean[][]")).is(
        factory.array(
            factory.array(
                Type.Primitive.BOOLEAN)));
  }

  @Test
  public void parseParameterized() {
    check(parser.parse("java.util.List<String>"))
        .is(factory.parameterized(
            factory.reference("java.util.List"),
            ImmutableList.of(
                factory.reference("String"))));

    check(parser.parse("java.util.Map<A, B>")).is(
        factory.parameterized(
            factory.reference("java.util.Map"),
            ImmutableList.of(
                parameters.variable("A"),
                parameters.variable("B"))));
  }

  @Test
  public void parseWildcards() {
    check(parser.parse("List<?>")).is(
        factory.parameterized(
            factory.reference("List"),
            ImmutableList.of(
                factory.extendsWildcard(Type.OBJECT))));

    check(parser.parse("Map<? extends List<? extends A>, ? super B>")).is(
        factory.parameterized(
            factory.reference("Map"),
            ImmutableList.of(
                factory.extendsWildcard(
                    factory.parameterized(
                        factory.reference("List"),
                        ImmutableList.of(
                            factory.extendsWildcard(
                                parameters.variable("A"))))),
                factory.superWildcard(
                    parameters.variable("B")))));
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParseJustArgument() {
    parser.parse("<A>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParseArgumentToVariable() {
    parser.parse("A<B>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParseArgumentsToArguments() {
    parser.parse("List<A><B>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParseParametrizedPrimitive() {
    parser.parse("boolean<A>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParseArgumentsToArrays() {
    parser.parse("boolean[]<B>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParseNakedWildcard() {
    parser.parse("? extends A");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParsePrimitiveWildcard() {
    parser.parse("List<? super int>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParseArrayWildcard() {
    parser.parse("List<? extends Object[]>");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantParsePrimitiveArguments() {
    parser.parse("Map<boolean, int>");
  }
}
