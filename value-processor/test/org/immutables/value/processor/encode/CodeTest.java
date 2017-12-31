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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.immutables.value.processor.encode.Structurizer.Statement;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class CodeTest {
  @Test
  public void scan() {
    List<Code.Term> terms = Code.termsFrom("/* comment */ and.it.is('a', a + 1) so // distant\n \"aaaa\".ex() {}");
    StringBuilder sb = new StringBuilder();
    for (Code.Term term : terms) {
      if (term.isDelimiter()) {
        sb.append("|D|");
      } else if (term.isWhitespace()) {
        sb.append("|S|");
      } else if (term.isWordOrNumber()) {
        sb.append("|W|");
      } else {
        sb.append("|O|");
      }
      sb.append(term);
    }

    check(sb).hasToString("|O|/* comment */|S| |W|and|D|.|W|it|D|.|W|is|D|(|O|'a'|D|,|S| |W|a|S| |D|+|S| |W|1|D|)"
        + "|S| |W|so|S| |O|// distant|S|\n |O|\"aaaa\"|D|.|W|ex|D|(|D|)|S| |D|{|D|}");
  }

  @Test
  public void bind() {
    List<Code.Term> terms =
        Code.termsFrom("this.aa(OPA.aa, CARNIVORE./* */aa, HERBIVORE. bb, UNIVORE. <My> aa()).and(My.class);this.HERBIVORE");

    Code.Binder binder = new Code.Binder(
        ImmutableMap.of("My", "xx.My"),
        ImmutableSet.of(Code.Binding.newTop("HERBIVORE"), Code.Binding.newMethod("aa")));

    String joined = Code.join(binder.apply(terms));

    check(joined).is("this.@:aa(OPA.aa, CARNIVORE./* */aa, @^HERBIVORE. bb, UNIVORE. <xx.My> aa()).and(xx.My.class);this.HERBIVORE");
  }

  @Test
  public void bind2() {
    List<Code.Term> terms = Code.termsFrom("this.aa\n(this\n.aa, this :: aa)");

    Code.Binder binder = new Code.Binder(
        ImmutableMap.<String, String>of(),
        ImmutableSet.of(Code.Binding.newMethod("aa"), Code.Binding.newField("aa")));

    String joined = Code.join(binder.apply(terms));

    check(joined).is("this.@:aa\n(this\n.@@aa, this :: @:aa)");
  }

  @Test
  public void bindWithNestedGenerics() {
    List<Code.Term> terms =
        Code.termsFrom("Optional.<Map<K, V>>fromNullable(null)");

    Code.Binder binder = new Code.Binder(
        ImmutableMap.<String, String>of(),
        ImmutableSet.of(Code.Binding.newTop("K"), Code.Binding.newTop("V")));

    String joined = Code.join(binder.apply(terms));
    check(joined).is("Optional.<Map<@^K, @^V>>fromNullable(null)");
  }

  @Test
  public void structurize() {
    SourceMapper m1 = new SourceMapper(""
        + "import z;\n"
        + "import o;\n"
        + "interface A {\n"
        + "int f /* */ = 1   +2;\n"
        + "@Deprecated class B {  static {} int u =1; @Nonnull(when = When.ALWAYS) String k= abc.a(); \n}"
        + " @Override String h() throws java.lang.Exception { return \"xxx\"; }"
        + " private abstract @Override void j() {}"
        + " private synchronized Gg<T, List< T>> g() {}"
        + "\n}");

    check(Code.join(m1.getExpression("A.f"))).hasToString("1   +2");
    check(Code.join(m1.getBlock("A.h()"))).hasToString("{ return \"xxx\"; }");
    check(Code.join(m1.getExpression("A.B.u"))).hasToString("1");
    check(Code.join(m1.getReturnType("A.h()"))).hasToString("String");
    check(Code.join(m1.getReturnType("A.g()"))).hasToString("Gg<T,List<T>>");
  }

  @Test
  public void oneLiner() {
    check(Code.join(Code.oneLiner(Code.termsFrom("{ \n return 1 + 1 ;// returns \n }"))))
        .hasToString("1 + 1");

    check(Code.join(Code.oneLiner(Code.termsFrom("{return 2 * 2;\n}"))))
        .hasToString("2 * 2");

    check(Code.oneLiner(Code.termsFrom("{ for (;;) { return 1 + 1; } }"))).isEmpty();
    check(Code.oneLiner(Code.termsFrom("{}"))).isEmpty();
  }

  @Test
  public void trimLeadingIndent() {
    check(Code.join(Code.trimLeadingIndent(Code.termsFrom("{ \n return 1 + 1 ;// returns \n }"))))
        .is("{ \nreturn 1 + 1 ;// returns \n}");
  }
}
