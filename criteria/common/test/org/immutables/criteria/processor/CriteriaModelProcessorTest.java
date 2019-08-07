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

import org.immutables.criteria.Criteria;
import org.immutables.value.processor.encode.Type;
import org.immutables.value.processor.meta.ProcessorRule;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;
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

public class CriteriaModelProcessorTest {

  @Rule
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
          attribute.criteria().buildMatcher();
          attribute.criteria().matcher().creator();
        } catch (Exception e) {
          throw new AssertionError(String.format("Failed generating matcher for attribute  %s: %s",
                  attribute.name(), e.getMessage()), e);
        }
      }
    }
  }

  @Test
  public void basic() {
    assertAttribute("string", "org.immutables.criteria.matcher.StringMatcher.Template<R>");
    assertAttribute("optionalString", "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.StringMatcher.Template<R>>");
    assertAttribute("stringList", "org.immutables.criteria.matcher.IterableMatcher<R,org.immutables.criteria.matcher.StringMatcher.Template<R>,java.lang.String>");


    assertAttribute("integer",
            "org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Integer>");
    assertAttribute("arrayInteger",
            "org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R, java.lang.Integer>,java.lang.Integer>");
    assertAttribute("arrayArrayInteger",
            "org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Integer>,java.lang.Integer>,java.lang.Integer[]>");
    assertAttribute("optionalInteger",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Integer>>");
    assertAttribute("optionalInteger2",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Integer>>");

    assertAttribute("longValue",
            "org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Long>");
    assertAttribute("optionalLong",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Long>>");
    assertAttribute("optionalLong2",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Long>>");

    assertAttribute("doubleValue",
            "org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Double>");
    assertAttribute("optionalDouble",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Double>>");
    assertAttribute("optionalDouble2",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Double>>");

    assertAttribute("booleanValue",
            "org.immutables.criteria.matcher.BooleanMatcher.Template<R>");
    assertAttribute("optionalBoolean",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.BooleanMatcher.Template<R>>");

    assertAttribute("timeZone",
            "org.immutables.criteria.matcher.ObjectMatcher.Template<R,java.util.TimeZone>");
    assertAttribute("optionalTimeZone",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ObjectMatcher.Template<R,java.util.TimeZone>>");
  }

  @Test
  public void array() {
    assertAttribute("arrayDouble",
            "org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R, java.lang.Double>,java.lang.Double>");

  }

  @Test
  public void wierd() {
    assertAttribute("weird1",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.StringMatcher.Template<R>>>");
    assertAttribute("weird2",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.IterableMatcher<R,org.immutables.criteria.matcher.StringMatcher.Template<R>,java.lang.String>>");
    assertAttribute("weird3",
            "org.immutables.criteria.matcher.IterableMatcher<R,org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.StringMatcher.Template<R>>,java.util.Optional<java.lang.String>>");
    assertAttribute("weird4",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R,java.lang.Integer>>>");
  }

  @Test
  public void havingCriteria() {
    assertAttribute("foo",
            "org.immutables.criteria.processor.CriteriaModelProcessorTest.FooCriteria<R>");
    assertAttribute("optionalFoo",
            "org.immutables.criteria.matcher.OptionalMatcher<R,org.immutables.criteria.processor.CriteriaModelProcessorTest.FooCriteria<R>>");
    assertAttribute("listFoo",
            "org.immutables.criteria.matcher.IterableMatcher<R,org.immutables.criteria.processor.CriteriaModelProcessorTest.FooCriteria<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>");
    assertAttribute("listListFoo",
            "org.immutables.criteria.matcher.IterableMatcher<R,org.immutables.criteria.matcher.IterableMatcher<R,org.immutables.criteria.processor.CriteriaModelProcessorTest.FooCriteria<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>,java.util.List<org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>>");
    assertAttribute("arrayFoo",
            "org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.processor.CriteriaModelProcessorTest.FooCriteria<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>");
    assertAttribute("arrayArrayFoo",
            "org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.processor.CriteriaModelProcessorTest.FooCriteria<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[]>");
    assertAttribute("listArrayFoo",
            "org.immutables.criteria.matcher.IterableMatcher<R,org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.processor.CriteriaModelProcessorTest.FooCriteria<R>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo>,org.immutables.criteria.processor.CriteriaModelProcessorTest.Foo[]>");
  }

  @Test
  public void debug() {
    assertAttribute("arrayInteger",
            "org.immutables.criteria.matcher.ArrayMatcher<R,org.immutables.criteria.matcher.ComparableMatcher.Template<R, java.lang.Integer>,java.lang.Integer>");
  }

  private void assertAttribute(String name, String expected) {
    ValueAttribute attribute = findAttribute(name);
    final Type element = attribute.criteria().buildMatcher();
    final UnaryOperator<String> stripFn = str -> str.replaceAll("\\s+", "");
    assertEquals(String.format("for attribute %s", name), stripFn.apply(expected), stripFn.apply(element.toString()));
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
    List<String> stringList();

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

    long longValue();
    long[] arrayLong();
    List<Long> listLong();
    OptionalLong optionalLong();
    Optional<Long> optionalLong2();

    double doubleValue();
    double[] arrayDouble();
    List<Double> listDouble();
    OptionalDouble optionalDouble();
    Optional<Double> optionalDouble2();

    boolean booleanValue();
    boolean[] arrayBoolean();
    List<Boolean> listBoolean();
    Optional<Boolean> optionalBoolean();

    // non-comparable
    TimeZone timeZone();
    Optional<TimeZone> optionalTimeZone();

    // attributes which have criteria defined
    Foo foo();
    Optional<Foo> optionalFoo();
    List<Foo> listFoo();
    List<List<Foo>> listListFoo();
    Foo[] arrayFoo();
    Foo[][] arrayArrayFoo();
    List<Foo[]> listArrayFoo();
  }

  @ProcessorRule.TestImmutable
  @Criteria
  interface Foo {}

}
