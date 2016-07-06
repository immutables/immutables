package org.immutables.value.processor.encode;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.immutables.generator.SourceExtraction;
import org.immutables.value.processor.encode.Code.Term;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class CodeTest {
  @Test
  public void scan() {
    List<Term> terms = Code.termsFrom("/* comment */ and.it.is('a', a + 1) so // distant\n \"aaaa\".ex() {}");
    StringBuilder sb = new StringBuilder();
    for (Term term : terms) {
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
        + "|S| |W|so|S| |O|// distant\n|S| |O|\"aaaa\"|D|.|W|ex|D|(|D|)|S| |D|{|D|}");
  }

  @Test
  public void bind() {
    List<Term> terms = Code.termsFrom("this.aa(CARNIVORE.aa, HERBIVORE.bb).and(My.class)");

    Code.Resolver binder =
        new Code.Resolver(
            SourceExtraction.importsFrom("import xx.My; class"),
            Predicates.in(ImmutableSet.of("aa", "HERBIVORE")));

    String joined = Joiner.on("").join(binder.resolve(terms));

    check(joined).is("this.@@aa(CARNIVORE.aa, @@HERBIVORE.bb).and(xx.My.class)");
  }
}
