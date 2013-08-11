/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.check;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.LinkedList;
import javax.annotation.Nullable;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;

/**
 * The match wrapper.
 * @param <T> the generic type
 */
public class ObjectChecker<T> {

  final T actualValue;
  final boolean negate;

  ObjectChecker(T actualValue, boolean negate) {
    this.actualValue = actualValue;
    this.negate = negate;
  }

  static void fail(@Nullable Object actualValue, Matcher<?> matcher) {
    Description description =
        new StringDescription()
            .appendText("\nExpected: ")
            .appendDescriptionOf(matcher)
            .appendText("\n     but: ");
    matcher.describeMismatch(actualValue, description);
    AssertionError assertionError = new AssertionError(description.toString());
    assertionError.setStackTrace(ObjectChecker.trimStackTrace(assertionError.getStackTrace()));
    throw assertionError;
  }

  static StackTraceElement[] trimStackTrace(StackTraceElement[] stackTrace) {
    LinkedList<StackTraceElement> list = Lists.newLinkedList();
    for (int i = stackTrace.length - 1; i >= 0; i--) {
      StackTraceElement s = stackTrace[i];
      if (s.getClassName().startsWith(Checkers.class.getPackage().getName())) {
        break;
      }
      list.addLast(s);
    }
    return list.toArray(new StackTraceElement[list.size()]);
  }

  static void verifyCheck(String description, boolean assertionState) {
    try {
      MatcherAssert.assertThat(description, assertionState);
    } catch (Throwable ex) {
      ex.setStackTrace(trimStackTrace(ex.getStackTrace()));
      Throwables.propagate(ex);
    }
  }

  static <Z> void verifyUsingMatcher(@Nullable Z actualValue, Matcher<? super Z> matcher) {
    try {
      MatcherAssert.assertThat(actualValue, matcher);
    } catch (Throwable ex) {
      ex.setStackTrace(trimStackTrace(ex.getStackTrace()));
      Throwables.propagate(ex);
    }
  }

  /**
   * Deprecated to prevent accidental usage of this method.
   * @param any object
   * @return nothing
   * @deprecated You probably want to use {@link #is(Object)} method for matcher wrapper.
   */
  @Override
  @Deprecated
  public boolean equals(Object any) {
    throw new UnsupportedOperationException("Use 'is' method on matcher wrapper");
  }

  /**
   * Evaluates to true only if ALL of the passed in matchers evaluate to true.
   * @param matchers the matchers
   */
  @SafeVarargs
  public final void allOf(Matcher<? super T>... matchers) {
    verifyUsingMatcher(CoreMatchers.allOf(matchers));
  }

  /**
   * Evaluates to true if ANY of the passed in matchers evaluate to true.
   * @param matchers the matchers
   */
  @SafeVarargs
  public final void anyOf(Matcher<? super T>... matchers) {
    verifyUsingMatcher(CoreMatchers.anyOf(matchers));
  }

  /** This matcher always evaluates to true. */
  public void anything() {
    verifyUsingMatcher(CoreMatchers.anything());
  }

  public StringChecker asString() {
    return new StringChecker(actualValue != null ? actualValue.toString() : null, negate);
  }

  public void hasToString(String expectedToStringValue) {
    asString().is(expectedToStringValue);
  }

  public void is(Matcher<? super T> matcher) {
    verifyUsingMatcher(matcher);
  }

  /**
   * This is equal to check
   * @param value the value
   */
  public void is(@Nullable T value) {
    if (value == null) {
      isNull();
    } else {
      verifyUsingMatcher(Is.is(value));
    }
  }

  /**
   * Is the value an instance of a particular type?.
   * @param type the type
   */
  public void isA(java.lang.Class<?> type) {
    verifyUsingMatcher(CoreMatchers.instanceOf(type));
  }

  public void isNull() {
    verifyUsingMatcher(IsNull.nullValue());
  }

  /**
   * Inverts the rule.
   * @param matcher the matcher
   */
  public void not(Matcher<? super T> matcher) {
    verifyUsingMatcher(CoreMatchers.not(matcher));
  }

  /**
   * Makes checker negative.
   * @return negative checker
   */
  public ObjectChecker<T> not() {
    ensureNonNegative();
    return new ObjectChecker<>(actualValue, true);
  }

  protected void ensureNonNegative() {
    if (negate) {
      throw new IllegalStateException("Already negated checker, simply check expression to avoid double negation");
    }
  }

  public void not(@Nullable T value) {
    if (value == null) {
      notNull();
    } else {
      verifyUsingMatcher(CoreMatchers.not(value));
    }
  }

  public void notNull() {
    verifyUsingMatcher(IsNull.notNullValue());
  }

  /**
   * Creates a new instance of IsSame
   * @param object The predicate evaluates to true only when the argument is this object.
   */
  public void same(T object) {
    verifyUsingMatcher(CoreMatchers.sameInstance(object));
  }

  public void isIn(Collection<T> values) {
    verifyUsingMatcher(Matchers.isIn(values));
  }

  @SafeVarargs
  public final void isIn(T... values) {
    verifyUsingMatcher(Matchers.isIn(values));
  }

  public void satisfies(Predicate<? super T> predicate) {
    if (negate) {
      predicate = Predicates.not(predicate);
    }
    verifyUsingMatcher(predicate.apply(actualValue), Is.is(true));
  }

  protected void verifyUsingMatcher(Matcher<? super T> matcher) {
    if (negate) {
      matcher = CoreMatchers.not(matcher);
    }
    verifyUsingMatcher(actualValue, matcher);
  }

  /**
   * @deprecated This class should not be used with collections
   */
  @Deprecated
  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("This class should not be used with collections");
  }
}
