/*
   Copyright 2013-2018 Immutables Authors and Contributors

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

import javax.annotation.Nullable;
import org.hamcrest.Matchers;
import org.hamcrest.text.IsEmptyString;
import org.junit.Assert;

/** The string match wrapper. */
public class StringChecker extends ObjectChecker<String> {

  StringChecker(@Nullable String actualValue, boolean negate) {
    super(actualValue, negate);
  }

  @Override
  public StringChecker not() {
    ensureNonNegative();
    return new StringChecker(actualValue, true);
  }

  public void contains(String substring) {
    verifyUsingMatcher(Matchers.containsString(substring));
  }

  public void endsWith(String suffix) {
    verifyUsingMatcher(Matchers.endsWith(suffix));
  }

  @Override
  public void is(String value) {
    try {
      Assert.assertEquals(value, actualValue);
    } catch (AssertionError ex) {
      // We do this handling to cut stack trace and lineup error message

      String message = ex.getMessage()
          .replace("expected:", "\nExpected: ")
          .replace("but was:", "\n     but: was ");

      verifyCheck(message, false);
    }
  }

  public void isEmpty() {
    verifyUsingMatcher(IsEmptyString.isEmptyString());
  }

  public void isNonEmpty() {
    verifyUsingMatcher(Matchers.not(IsEmptyString.isEmptyOrNullString()));
  }

  public void isNullOrEmpty() {
    verifyUsingMatcher(IsEmptyString.isEmptyOrNullString());
  }

  public void matches(String pattern) {
    String expectedButWasMessage = "\nExpected: string that match regex /" + pattern + "/" +
        "\n     but: was ";

    verifyCheck(expectedButWasMessage + "null", actualValue != null);
    assert actualValue != null;
    verifyCheck(expectedButWasMessage + "\"" + actualValue + "\"", actualValue.matches(pattern));
  }

  public void startsWith(String prefix) {
    verifyUsingMatcher(Matchers.startsWith(prefix));
  }

  public void notEmpty() {
    verifyUsingMatcher(Matchers.not(IsEmptyString.isEmptyOrNullString()));
  }
}
