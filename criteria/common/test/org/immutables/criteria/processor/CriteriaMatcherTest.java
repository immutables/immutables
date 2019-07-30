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

import org.immutables.value.processor.meta.AttributeCriteria;
import org.immutables.value.processor.meta.ProcessorRule;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TimeZone;

import static org.immutables.check.Checkers.check;

public class CriteriaMatcherTest {

  @Rule
  public final ProcessorRule rule = new ProcessorRule();

  @Test
  public void string() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("string");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.StringMatcher.Template<R>");
    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.StringMatcher.Self");
  }

  @Test
  public void optionalString() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("optionalString");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher<R, " +
            "org.immutables.criteria.matcher.StringMatcher.Template<R>>");

    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher.Self<" +
            "org.immutables.criteria.matcher.StringMatcher.Self>");
  }

  @Test
  public void stringList() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("stringList");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.IterableMatcher<R, org.immutables.criteria.matcher.StringMatcher.Template<R>, java.lang.String>");
    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.IterableMatcher.Self<org.immutables.criteria.matcher.StringMatcher.Self, java.lang.String>");

  }

  @Test
  public void integer() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("integer");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.ComparableMatcher.Template<R, java.lang.Integer>");
    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.ComparableMatcher.Self<java.lang.Integer>");
  }

  @Test
  public void optionalInteger() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("optionalInteger");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher<R, " +
            "org.immutables.criteria.matcher.ComparableMatcher.Template<R, java.lang.Integer>>");

    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher.Self<" +
            "org.immutables.criteria.matcher.ComparableMatcher.Self<java.lang.Integer>>");
  }

  @Test
  public void booleanValue() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("booleanValue");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.BooleanMatcher.Template<R>");
    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.BooleanMatcher.Self");
  }

  @Test
  public void optionalBoolean() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("optionalBoolean");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher<R, " +
            "org.immutables.criteria.matcher.BooleanMatcher.Template<R>>");
    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher.Self<org.immutables.criteria.matcher.BooleanMatcher.Self>");
  }

  @Test
  public void timeZone() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("timeZone");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.ObjectMatcher.Template<R, java.util.TimeZone>");
    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.ObjectMatcher.Self<java.util.TimeZone>");
  }

  @Test
  public void optionalTimeZone() {
    AttributeCriteria.MatcherDefinition matcher = matcherFor("optionalTimeZone");
    check(matcher.toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher<R, " +
            "org.immutables.criteria.matcher.ObjectMatcher.Template<R, java.util.TimeZone>>");

    check(matcher.toSelf().toTemplate()).is("org.immutables.criteria.matcher.OptionalMatcher.Self<" +
            "org.immutables.criteria.matcher.ObjectMatcher.Self<java.util.TimeZone>>");
  }

  private AttributeCriteria.MatcherDefinition matcherFor(String name) {
    ValueType type = rule.value(Model.class);
    ValueAttribute attr = findAttribute(type, name);
    return attr.criteria().matcher();
  }

  private ValueAttribute findAttribute(ValueType type, String name) {
    for (ValueAttribute attr: type.attributes) {
      if (attr.name().equals(name)) {
        return attr;
      }
    }

    throw new IllegalArgumentException(String.format("%s not found in %s", name, type.name()));
  }

  @ProcessorRule.TestImmutable
  interface Model {
     String string();
     Optional<String> optionalString();
     List<String> stringList();

     int integer();
     OptionalInt optionalInteger();

     boolean booleanValue();
     Optional<Boolean> optionalBoolean();

     // non-comparable
     TimeZone timeZone();
     Optional<TimeZone> optionalTimeZone();
  }


}