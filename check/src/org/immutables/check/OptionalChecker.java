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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * The optional match wrapper.
 * @param <T> the generic type
 */
public class OptionalChecker<T> {

  private final Optional<T> optionalValue;

  OptionalChecker(Optional<T> optionalValue) {
    this.optionalValue = Preconditions.checkNotNull(optionalValue);
  }

  public StringChecker asString() {
    return new StringChecker(
        optionalValue.isPresent()
            ? optionalValue.get().toString()
            : null);
  }

  public void isAbsent() {
    ObjectChecker.verifyCheck("\nExpected: Optional.absent()" +
        "\n     but: was " + optionalValue, !optionalValue.isPresent());
  }

  public void isOf(Object object) {
    of(Matchers.equalTo(Preconditions.checkNotNull(object)));
  }

  public void isPresent() {
    ObjectChecker.verifyCheck("\nExpected: Optional.of(*)" +
        "\n     but: was Optional.absent()",
        optionalValue.isPresent());
  }

  public void of(Matcher<? super T> matcher) {
    if (optionalValue.isPresent()) {
      Checkers.check(optionalValue.get()).is(matcher);
    }
    else {
      // We fail any matcher on absent value in this check
      ObjectChecker.fail(optionalValue, matcher);
    }
  }
}
