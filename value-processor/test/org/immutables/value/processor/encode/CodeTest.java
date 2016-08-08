package org.immutables.value.processor.encode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
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
        Code.termsFrom("this.aa(OPA.aa, CARNIVORE./* */aa, HERBIVORE. bb, UNIVORE. <My> aa).and(My.class);this.HERBIVORE");

    Code.Binder binder =
        new Code.Binder(
            ImmutableMap.of("My", "xx.My"),
            ImmutableSet.of("aa"),
            ImmutableSet.of("HERBIVORE"));

    String joined = Code.join(binder.apply(terms));

    check(joined).is("this.@@aa(OPA.aa, CARNIVORE./* */aa, @^HERBIVORE. bb, UNIVORE. <xx.My> aa).and(xx.My.class);this.HERBIVORE");
  }

  @Test
  public void bindWithNestedGenerics() {
    List<Code.Term> terms =
        Code.termsFrom("Optional.<Map<K, V>>fromNullable(null)");

    Code.Binder binder =
        new Code.Binder(
            ImmutableMap.<String, String>of(),
            ImmutableSet.of("K", "V"),
            ImmutableSet.<String>of());

    String joined = Code.join(binder.apply(terms));
    check(joined).is("Optional.<Map<@@K, @@V>>fromNullable(null)");
  }

  @Test
  public void structurize() {
    SourceMapper m1 = new SourceMapper(""
        + "import z;\n"
        + "import o;\n"
        + "class A {\n"
        + "int f /* */ = 1   +2;\n"
        + "@Deprecated class B {  static {} int u =1; @Nonnull(when = When.ALWAYS) String k= abc.a(); \n}"
        + " @Override String h() throws java.lang.Exception { return \"xxx\"; }"
        + "\n}");

    check(Code.join(m1.getExpression("A.f"))).hasToString("1   +2");
    check(Code.join(m1.getBlock("A.h"))).hasToString("{ return \"xxx\"; }");
    check(Code.join(m1.getExpression("A.B.u"))).hasToString("1");
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
