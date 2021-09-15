/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.value.processor.encode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.immutables.value.processor.encode.Type.Defined;
import org.immutables.value.processor.encode.Type.Primitive;
import org.immutables.value.processor.encode.Type.Reference;
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
  public void parseUnderscoreType() {
    check(parser.parse("_Type")).hasToString("_Type");
  }

  @Test
  public void parseIngoreTypeAnnotation() {
    check(parser.parse("@type.annotation.TypeA @TypeD int")).is(Primitive.INT);
    check(parser.parse("java.lang.@type.annotation.TypeA @type.annotation.TypeB @TypeC String")).is(Reference.STRING);
  }

  @Test
  public void parseIngoreTypeAnnotationWithParameters() {
    check(parser.parse("java.lang.@javax.validation.constraints.Size(max = 10)String")).is(Reference.STRING);
    check(parser.parse("java.lang.@javax.validation.constraints.Size(max = 10, @A(a=@B(b=1))) String")).is(Reference.STRING);
    check(parser.parse("@Ann(\"b\") int")).is(Primitive.INT);
    check(parser.parse("(@javax.validation.constraints.NotNull :: java.lang.String)")).is(Reference.STRING);
    check(parser.parse("(@javax.validation.constraints.NotNull :: byte)[]")).is(factory.array(Primitive.BYTE));
    check(parser.parse("(@javax.validation.constraints.NotNull:: java.lang.String)[][]"))
        .is(factory.array(factory.array(Reference.STRING)));
  }

  @Test
  public void templateMoreMatches() {
    check(!new Type.Template(Reference.STRING).match(Reference.OBJECT).isPresent());
    check(new Type.Template(parser.parse("A")).match(Reference.OBJECT).isPresent());
    check(new Type.Template(parser.parse("B")).match(Reference.STRING).isPresent());

    VariableResolver nestedMatch =
        new Type.Template(parser.parse("Map<A, List<B>>")).match(parser.parse("Map<java.lang.String, List<D>>")).get();

    check(nestedMatch.variable(parameters.variable("A"))).is(Reference.STRING);
    check(nestedMatch.variable(parameters.variable("B"))).is(parameters.variable("D"));
  }

  @Test
  public void resolveVariable() {
    Type map = parser.parse("java.util.Map<A, B>");

    Type.VariableResolver resolver = Type.VariableResolver.empty()
        .bind(parameters.variable("A"), Type.OBJECT)
        .bind(parameters.variable("B"), parameters.introduce("C", NO_BOUNDS).variable("C"));

    Type substituted = map.accept(resolver);

    check(substituted).hasToString("java.util.Map<java.lang.Object, C>");
  }

  @Test
  public void parseBasics() {
    check(parser.parse("boolean")).is(Type.Primitive.BOOLEAN);
    check(parser.parse("void")).is(Type.Primitive.VOID);
    check(parser.parse("java.lang.Object")).same(Type.OBJECT);
    check(parser.parse("java.util.List")).same(parser.parse("java.util.List"));
    check(parser.parse("boolean...")).is(factory.varargs(Type.Primitive.BOOLEAN));
    check(parser.parse("java.lang.Object...")).is(factory.varargs(Type.OBJECT));
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

    check(parser.parse("java.util.Vector<Integer>..."))
        .is(factory.varargs(
            factory.parameterized(
                factory.reference("java.util.Vector"),
                ImmutableList.of(
                    factory.reference("Integer")))));

    check(parser.parse("java.util.Vector<Integer>[][]..."))
        .is(factory.varargs(
            factory.array(
                factory.array(
                    factory.parameterized(
                        factory.reference("java.util.Vector"),
                        ImmutableList.of(
                            factory.reference("Integer")))))));

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
  public void cantParseNonLastVarargs() {
    parser.parse("A<B>...[]");
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
