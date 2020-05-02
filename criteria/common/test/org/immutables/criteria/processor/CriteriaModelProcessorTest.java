/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.processor;

import org.immutables.check.Checkers;
import org.immutables.check.StringChecker;
import org.immutables.criteria.Criteria;
import org.immutables.value.processor.encode.Type;
import org.immutables.value.processor.meta.ProcessorRule;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import static org.junit.Assert.assertEquals;

/**
 * Validates field types and constructors of Matchers.
 *
 * <p>The reason this was not yet migrated to JUnit5 is because (apparently) tests are executed
 * in different thread from compiler which makes hard to get correct {@link javax.annotation.processing.ProcessingEnvironment}.
 * </p>
 * @see <a href="https://github.com/Kiskae/compile-testing-extension">Junit5 compile extension</a>
 * @see <a href="https://github.com/google/compile-testing/pull/155">Add JUnit5 implementation of CompilationRule</a>
 */
public class CriteriaModelProcessorTest {

  @Rule // TODO migrate to JUnit5 Extension
  public final ProcessorRule rule = new ProcessorRule();

  /**
   * Scans all attributes using reflection and generates a matcher
   */
  @Test
  public void scanAll() {
    for (Method method: Model.class.getDeclaredMethods()) {
      if (method.getParameterCount() == 0) {
        final ValueAttribute attribute = findAttribute(method.getName());
        try {
          attribute.criteria().matcher().creator();
        } catch (Exception e) {
          throw new AssertionError(String.format("Failed generating matcher for attribute  %s: %s",
                  attribute.name(), e.getMessage()), e);
        }
      }
    }
  }

  @Test
  public void timeZone() {

    assertAttribute("timeZone",
            "org.immutables.criteria.matcher.ObjectMatcher.Template<R,java.util.TimeZone>");
    assertAttribute("optionalTimeZone",
            "org.immutables.criteria.matcher.OptionalMatcher.Template<R,org.immutables.criteria.matcher.ObjectMatcher.Template<R,java.util.TimeZone>,java.util.Optional<java.util.TimeZone>>");
    checkCreator("optionalTimeZone").contains("ObjectMatcher.creator()");
    assertAttribute("nullableTimeZone",
            "org.immutables.criteria.matcher.OptionalMatcher.Template<R,org.immutables.criteria.matcher.ObjectMatcher.Template<R,java.util.TimeZone>,java.util.TimeZone>");
    checkCreator("nullableTimeZone").contains("ObjectMatcher.creator()");
    checkCreator("listTimeZone").contains("ObjectMatcher.creator()");
    checkCreator("arrayTimeZone").contains("ObjectMatcher.creator()");
  }

  @Test
  public void array() {
    assertAttribute("arrayDouble",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.DoubleMatcher.Template<R>,java.lang.Double,double[]>");

  }

  @Test
  public void wierd() {
    assertAttribute("weird1",
            "org.immutables.criteria.matcher.OptionalMatcher.Template<R,org.immutables.criteria.matcher.OptionalStringMatcher.Template<R,java.util.Optional<java.lang.String>>,java.util.Optional<java.util.Optional<java.lang.String>>>");
    // Optional<List<String>> weird2();
    assertAttribute("weird2",
            "org.immutables.criteria.matcher.OptionalMatcher.Template<R,org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.StringMatcher.Template<R>,java.lang.String,java.util.List<java.lang.String>>,java.util.Optional<java.util.List<java.lang.String>>>");
    // List<Optional<String>> weird3();
    assertAttribute("weird3",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.OptionalStringMatcher.Template<R,java.util.Optional<java.lang.String>>,java.util.Optional<java.lang.String>,java.util.List<java.util.Optional<java.lang.String>>>");
    assertAttribute("weird4",
            "org.immutables.criteria.matcher.OptionalMatcher.Template<R,org.immutables.criteria.matcher.OptionalIntegerMatcher.Template<R,java.util.OptionalInt>,java.util.Optional<java.util.OptionalInt>>");
  }

  @Test
  public void forLong() {
    assertAttribute("longValue",
            "org.immutables.criteria.matcher.LongMatcher.Template<R>");
    assertAttribute("optionalLong",
            "org.immutables.criteria.matcher.OptionalLongMatcher.Template<R,java.util.OptionalLong>");
    assertAttribute("optionalLong2",
            "org.immutables.criteria.matcher.OptionalLongMatcher.Template<R,java.util.Optional<java.lang.Long>>");
    assertAttribute("nullableLong",
            "org.immutables.criteria.matcher.OptionalLongMatcher.Template<R,java.lang.Long>");
  }

  @Test
  public void forDouble() {
    assertAttribute("doubleValue",
            "org.immutables.criteria.matcher.DoubleMatcher.Template<R>");
    assertAttribute("optionalDouble",
            "org.immutables.criteria.matcher.OptionalDoubleMatcher.Template<R,java.util.OptionalDouble>");
    assertAttribute("optionalDouble2",
            "org.immutables.criteria.matcher.OptionalDoubleMatcher.Template<R,java.util.Optional<java.lang.Double>>");
    assertAttribute("nullableDouble",
            "org.immutables.criteria.matcher.OptionalDoubleMatcher.Template<R,java.lang.Double>");
  }

  @Test
  public void forInteger() {
    assertAttribute("integer",
            "org.immutables.criteria.matcher.IntegerMatcher.Template<R>");
    assertAttribute("arrayInteger",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.IntegerMatcher.Template<R>,java.lang.Integer, int[]>");
    assertAttribute("arrayArrayInteger",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.IntegerMatcher.Template<R>,java.lang.Integer, int[]>,java.lang.Integer[], int[][]>");
    assertAttribute("optionalInteger",
            "org.immutables.criteria.matcher.OptionalIntegerMatcher.Template<R,java.util.OptionalInt>");
    assertAttribute("optionalInteger2",
            "org.immutables.criteria.matcher.OptionalIntegerMatcher.Template<R,java.util.Optional<java.lang.Integer>>");
    assertAttribute("nullableInteger",
            "org.immutables.criteria.matcher.OptionalIntegerMatcher.Template<R,java.lang.Integer>");
  }

  @Test
  public void bigInteger() {
    assertAttribute("bigInteger", "org.immutables.criteria.matcher.BigIntegerMatcher.Template<R>");
    assertAttribute("arrayBigInteger", "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.BigIntegerMatcher.Template<R>,java.math.BigInteger,java.math.BigInteger[]>");
    assertAttribute("listBigInteger", "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.BigIntegerMatcher.Template<R>,java.math.BigInteger,java.util.List<java.math.BigInteger>>");
    assertAttribute("optionalBigInteger", "org.immutables.criteria.matcher.OptionalBigIntegerMatcher.Template<R,java.util.Optional<java.math.BigInteger>>");
    assertAttribute("nullableBigInteger", "org.immutables.criteria.matcher.OptionalBigIntegerMatcher.Template<R,java.math.BigInteger>");
  }

  @Test
  public void bigDecimal() {
    assertAttribute("bigDecimal", "org.immutables.criteria.matcher.BigDecimalMatcher.Template<R>");
    assertAttribute("arrayBigDecimal", "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.BigDecimalMatcher.Template<R>,java.math.BigDecimal,java.math.BigDecimal[]>");
    assertAttribute("listBigDecimal", "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.BigDecimalMatcher.Template<R>,java.math.BigDecimal,java.util.List<java.math.BigDecimal>>");
    assertAttribute("optionalBigDecimal", "org.immutables.criteria.matcher.OptionalBigDecimalMatcher.Template<R,java.util.Optional<java.math.BigDecimal>>");
    assertAttribute("nullableBigDecimal", "org.immutables.criteria.matcher.OptionalBigDecimalMatcher.Template<R,java.math.BigDecimal>");
  }

  @Test
  public void forBoolean() {
    assertAttribute("booleanValue",
            "org.immutables.criteria.matcher.BooleanMatcher.Template<R>");
    assertAttribute("optionalBoolean",
            "org.immutables.criteria.matcher.OptionalBooleanMatcher.Template<R, java.util.Optional<java.lang.Boolean>>");
    assertAttribute("nullableBoolean",
            "org.immutables.criteria.matcher.OptionalBooleanMatcher.Template<R, java.lang.Boolean>");
  }

  @Test
  public void string() {
    assertAttribute("string", "org.immutables.criteria.matcher.StringMatcher.Template<R>");
    checkCreator("string").not().contains("ModelCriteria.creator()");
    assertAttribute("nullableString", "org.immutables.criteria.matcher.OptionalStringMatcher.Template<R, java.lang.String>");
    assertAttribute("optionalString", "org.immutables.criteria.matcher.OptionalStringMatcher.Template<R,java.util.Optional<java.lang.String>>");
    assertAttribute("guavaOptionalString", "org.immutables.criteria.matcher.OptionalStringMatcher.Template<R,com.google.common.base.Optional<java.lang.String>>");
    assertAttribute("fugue3OptionalString", "org.immutables.criteria.matcher.OptionalStringMatcher.Template<R,io.atlassian.fugue.Option<java.lang.String>>");
    assertAttribute("stringList", "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.StringMatcher.Template<R>,java.lang.String,java.util.List<java.lang.String>>");
    checkCreator("stringList").contains("IterableMatcher.creator()");
    checkCreator("arrayList").contains("IterableMatcher.creator()");

  }

  @Test
  public void creatorDefinition() {
    checkCreator("arrayList").contains("creator().create(context.appendPath(");
  }

  @Test
  public void havingCriteria() {
    // Criteria should be generated as top-level class (not nested).
    assertAttribute("foo",
            "org.immutables.criteria.processor.FooCriteriaTemplate<R>");

    checkCreator("foo").not().contains("FooCriteriaTemplate.creator()");
    checkCreator("foo").contains("FooCriteria.creator()");


    assertAttribute("nullableFoo",
            "org.immutables.criteria.matcher.OptionalMatcher.Template<R,org.immutables.criteria.processor.FooCriteriaTemplate<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>");

    checkCreator("nullableFoo").contains("FooCriteria.creator()");
    checkCreator("nullableFoo").contains("OptionalMatcher.creator()");

    assertAttribute("optionalFoo",
            "org.immutables.criteria.matcher.OptionalMatcher.Template<R,org.immutables.criteria.processor.FooCriteriaTemplate<R>,java.util.Optional<org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>>");

    checkCreator("optionalFoo").contains("FooCriteria.creator()");
    checkCreator("optionalFoo").contains("OptionalMatcher.creator()");

    assertAttribute("listFoo",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.processor.FooCriteriaTemplate<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo,java.util.List<org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>>");

    checkCreator("listFoo").contains("FooCriteria.creator()");
    checkCreator("listFoo").contains("IterableMatcher.creator()");

    assertAttribute("listListFoo",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.processor.FooCriteriaTemplate<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo,java.util.List<org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>>,java.util.List<org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>,java.util.List<java.util.List<org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>>>");
    assertAttribute("arrayFoo",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.processor.FooCriteriaTemplate<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[]>");
    assertAttribute("arrayArrayFoo",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.processor.FooCriteriaTemplate<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[]>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[],org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[][]>");
    assertAttribute("listArrayFoo",
            "org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.matcher.IterableMatcher.Template<R,org.immutables.criteria.processor.FooCriteriaTemplate<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[]>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[],java.util.List<org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[]>>");
  }

  @Test
  public void topLevel() {
    assertAttribute("topLevel",
            "org.immutables.criteria.processor.TopLevelCriteriaTemplate<R>");
  }

  private void assertAttribute(String name, String expected) {
    ValueAttribute attribute = findAttribute(name);
    final Type element = attribute.criteria().matcher().matcherType();
    final UnaryOperator<String> stripFn = str -> str.replaceAll("\\s+", "");
    assertEquals(String.format("for attribute %s", name), stripFn.apply(expected), stripFn.apply(element.toString()));
  }

  private StringChecker checkCreator(String name) {
    ValueAttribute attribute = findAttribute(name);
    final String creator = attribute.criteria().matcher().creator();
    final UnaryOperator<String> stripFn = str -> str.replaceAll("\\s+", "");
    return Checkers.check(stripFn.apply(creator));
  }

  private ValueAttribute findAttribute(String name) {
    Objects.requireNonNull(name, "name");
    final ValueType type = rule.value(Model.class);
    return type.attributes.stream().filter(a -> a.name().equals(name))
            .findAny().orElseThrow(() -> new NoSuchElementException(name + " not found in " + Model.class.getSimpleName()));
  }

  @ProcessorRule.TestImmutable
  @Criteria
  interface Model {
    String string();
    Optional<String> optionalString();
    com.google.common.base.Optional<String> guavaOptionalString();
    io.atlassian.fugue.Option<String> fugue3OptionalString();
    @Nullable String nullableString();
    List<String> stringList();
    String[] arrayList();

    Optional<Optional<String>> weird1();
    Optional<List<String>> weird2();
    List<Optional<String>> weird3();
    Optional<OptionalInt> weird4();

    int integer();
    int[] arrayInteger();
    int[][] arrayArrayInteger();
    List<Integer> listInteger();
    OptionalInt optionalInteger();
    Optional<Integer> optionalInteger2();
    @Nullable Integer nullableInteger();

    long longValue();
    long[] arrayLong();
    List<Long> listLong();
    OptionalLong optionalLong();
    Optional<Long> optionalLong2();
    @Nullable Long nullableLong();

    double doubleValue();
    double[] arrayDouble();
    List<Double> listDouble();
    OptionalDouble optionalDouble();
    Optional<Double> optionalDouble2();
    @Nullable Double nullableDouble();

    // == BigDecimal
    BigDecimal bigDecimal();
    BigDecimal[] arrayBigDecimal();
    List<BigDecimal> listBigDecimal();
    Optional<BigDecimal> optionalBigDecimal();
    @Nullable BigDecimal nullableBigDecimal();

    // == BigInteger
    BigInteger bigInteger();
    BigInteger[] arrayBigInteger();
    List<BigInteger> listBigInteger();
    Optional<BigInteger> optionalBigInteger();
    @Nullable BigInteger nullableBigInteger();


    boolean booleanValue();
    boolean[] arrayBoolean();
    List<Boolean> listBoolean();
    Optional<Boolean> optionalBoolean();
    @Nullable Boolean nullableBoolean();

    // non-comparable
    TimeZone timeZone();
    @Nullable TimeZone nullableTimeZone();
    Optional<TimeZone> optionalTimeZone();
    List<TimeZone> listTimeZone();
    TimeZone[] arrayTimeZone();

    // attributes which have criteria defined
    Foo foo();
    @Nullable Foo nullableFoo();
    Optional<Foo> optionalFoo();
    List<Foo> listFoo();
    List<List<Foo>> listListFoo();
    Foo[] arrayFoo();
    Foo[][] arrayArrayFoo();
    List<Foo[]> listArrayFoo();

    TopLevel topLevel();
  }

  @ProcessorRule.TestImmutable
  @Criteria
  interface Foo {}

}
