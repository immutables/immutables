/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.generator;

import org.immutables.generator.Naming.Preference;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class NamingTest {
  @Test(expected = IllegalArgumentException.class)
  public void wrongPlaceholder() {
    Naming.from("**");
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrongChars() {
    Naming.from("???");
  }

  @Test
  public void verbatimNaming() {
    Naming aa = Naming.from("aa");

    check(aa.apply("x")).is("aa");
    check(aa.apply("aa")).is("aa");
    check(aa.detect("aa")).is("aa");
    check(aa.detect("ff")).isEmpty();
    check(aa.detect("aaa")).isEmpty();
  }

  @Test
  public void prefixNaming() {
    Naming set = Naming.from("set*");

    check(set.apply("x")).is("setX");
    check(set.apply("X")).is("setX");
    check(set.apply("__")).is("set__");

    check(set.detect("se")).isEmpty();
    check(set.detect("set")).isEmpty();
    check(set.detect("sets")).isEmpty();

    check(set.detect("setSe")).is("se");
    check(set.detect("setXXX")).is("xXX");
  }

  @Test
  public void suffixNaming() {
    Naming added = Naming.from("*Added");

    check(added.apply("x")).is("xAdded");
    check(added.apply("X")).is("XAdded");
    check(added.apply("__")).is("__Added");

    check(added.detect("Added")).isEmpty();
    check(added.detect("dded")).isEmpty();

    check(added.detect("moreAdded")).is("more");
    check(added.detect("XAdded")).is("X");
    check(added.detect("XXXAdded")).is("XXX");
  }

  @Test
  public void prefixAndSuffixNaming() {
    Naming oneOf = Naming.from("one*Of");

    check(oneOf.apply("x")).is("oneXOf");
    check(oneOf.apply("X")).is("oneXOf");
    check(oneOf.apply("__")).is("one__Of");

    check(oneOf.detect("oneOf")).isEmpty();
    check(oneOf.detect("oneX")).isEmpty();
    check(oneOf.detect("xOf")).isEmpty();
    check(oneOf.detect("oneXOf")).is("x");
  }

  @Test
  public void underscoreCharacter() {
    Naming oneOf = Naming.from("Builder_*");

    check(oneOf.apply("x")).is("Builder_X");
    check(oneOf.apply("X")).is("Builder_X");
    check(oneOf.apply("__")).is("Builder___");

    check(oneOf.detect("Builder_X")).is("x");
    check(oneOf.detect("Builder__X")).isEmpty();
    check(oneOf.detect("BuilderX")).isEmpty();
  }

  @Test
  public void underscoreNaming() {
    Naming underscoreStar = Naming.from("_*");
    String detect = underscoreStar.detect("_Abacus");
    check(detect).is("abacus");

  }

  @Test
  public void sameNaming() {
    Naming star = Naming.from("*");
    check(star.apply("x")).is("x");
    check(star.detect("x")).is("x");
    check(star.detect("__")).is("__");
    check(star).same(Naming.identity());
  }

  @Test
  public void requireNonConstant() {
    check(Naming.identity().requireNonConstant(Preference.PREFIX)).same(Naming.identity());
    check(Naming.from("Create").requireNonConstant(Preference.PREFIX).apply("x")).is("CreateX");
    check(Naming.from("Create").requireNonConstant(Preference.SUFFIX).apply("x")).is("xCreate");
    check(Naming.from("new*").requireNonConstant(Preference.SUFFIX).apply("x")).is("newX");
  }

  @Test
  public void lowercaseSuffix() {
    check(Naming.from("check*out").detect("checkThisout")).is("this");
    check(Naming.from("check*out").apply("it")).is("checkItout");
  }

  @Test
  public void usageCorrection() {
    String suffix = Naming.from("of").requireNonConstant(Preference.SUFFIX).apply("Hen");
    check(Naming.Usage.LOWERIZED.apply(suffix)).is("henOf");

    String prefix = Naming.from("of").requireNonConstant(Preference.PREFIX).apply("Hen");
    check(Naming.Usage.CAPITALIZED.apply(prefix)).is("OfHen");
  }
}
