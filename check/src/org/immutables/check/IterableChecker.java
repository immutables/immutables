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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.matchers.JUnitMatchers;

/**
 * The iterable match wrapper.
 * @param <Z> the generic type
 * @param <T> the generic type
 */
public class IterableChecker<Z extends Iterable<T>, T> extends ObjectChecker<Z> {

  IterableChecker(Z value) {
    super(value);
  }

  public void any(Matcher<? extends T> elementMatcher) {
    // better output than hamcrest
    verifyUsingMatcher(JUnitMatchers.hasItem(elementMatcher));
  }

  public void every(Matcher<T> elementMatcher) {
    // better output than hamcrest
    verifyUsingMatcher(JUnitMatchers.everyItem(elementMatcher));
  }

  public void has(T element) {
    // better output than hamcrest hasItem
    verifyUsingMatcher(JUnitMatchers.hasItem(element));
  }

  @SafeVarargs
  public final void hasAll(T... element) {
    // better output than hamcrest hasItem
    verifyUsingMatcher(JUnitMatchers.hasItems(element));
  }

  public void isOf(Iterable<?> elements) {
    verifyUsingMatcher(Matchers.contains(Iterables.toArray(elements, Object.class)));
  }

  @SafeVarargs
  public final void isOf(T... elements) {
    verifyUsingMatcher(Matchers.contains(elements));
  }

  @SuppressWarnings("unchecked")
  public void hasContentInAnyOrder(Iterable<?> elements) {
    verifyUsingMatcher((Matcher<? super Z>) Matchers.containsInAnyOrder(ImmutableList.copyOf(elements)));
  }

  @SafeVarargs
  public final void hasContentInAnyOrder(T... elements) {
    verifyUsingMatcher(Matchers.containsInAnyOrder(elements));
  }

  public void hasSize(int size) {
    verifyUsingMatcher(ImmutableList.copyOf(actualValue), Matchers.hasSize(size));
  }

  public void notEmpty() {
    verifyUsingMatcher(Matchers.not(Matchers.<T>emptyIterable()));
  }

  public void isEmpty() {
    verifyUsingMatcher(Matchers.<T>emptyIterable());
  }

}
