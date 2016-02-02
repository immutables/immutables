/*
   Copyright 2013-2014 Immutables Authors and Contributors

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
import javax.annotation.Nullable;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * The iterable match wrapper.
 * @param <Z> the generic type
 * @param <T> the generic type
 */
@SuppressWarnings("unchecked")
public class IterableChecker<Z extends Iterable<T>, T> extends ObjectChecker<Z> {

  IterableChecker(@Nullable Z value, boolean negate) {
    super(value, negate);
  }

  @Override
  public IterableChecker<Z, T> not() {
    ensureNonNegative();
    return new IterableChecker<>(actualValue, true);
  }

  public void any(Matcher<? super T> elementMatcher) {
    verifyUsingMatcher(CoreMatchers.hasItem(elementMatcher));
  }

  public void every(Matcher<T> elementMatcher) {
    verifyUsingMatcher(CoreMatchers.everyItem(elementMatcher));
  }

  public void has(T element) {
    verifyUsingMatcher(CoreMatchers.hasItem(element));
  }

  @SafeVarargs
  public final void hasAll(T... elements) {
    verifyUsingMatcher(CoreMatchers.hasItems(elements));
  }

  public final void hasAll(Iterable<? extends T> elements) {
    verifyUsingMatcher(CoreMatchers.hasItems((T[]) Iterables.toArray(elements, Object.class)));
  }

  public void isOf(Iterable<?> elements) {
    verifyUsingMatcher(Matchers.contains(Iterables.toArray(elements, Object.class)));
  }

  @SafeVarargs
  public final void isOf(T... elements) {
    verifyUsingMatcher(Matchers.contains(elements));
  }

  public void hasContentInAnyOrder(Iterable<?> elements) {
    verifyUsingMatcher((Matcher<? super Z>) Matchers.containsInAnyOrder(ImmutableList.copyOf(elements)));
  }

  @SafeVarargs
  public final void hasContentInAnyOrder(T... elements) {
    verifyUsingMatcher(Matchers.containsInAnyOrder(elements));
  }

  public void hasSize(int size) {
    new IterableChecker<>(ImmutableList.copyOf(actualValue), negate).verifyUsingMatcher(Matchers.hasSize(size));
  }

  public void notEmpty() {
    verifyUsingMatcher(Matchers.not(Matchers.<T>emptyIterable()));
  }

  public void isEmpty() {
    verifyUsingMatcher(Matchers.<T>emptyIterable());
  }

}
