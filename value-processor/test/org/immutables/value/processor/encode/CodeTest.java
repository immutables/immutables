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
				+ "|S| |W|so|S| |O|// distant\n|S| |O|\"aaaa\"|D|.|W|ex|D|(|D|)|S| |D|{|D|}");
	}

	@Test
	public void bind() {
		List<Code.Term> terms =
				Code.termsFrom("this.aa(OPA.aa, CARNIVORE./* */aa, HERBIVORE. bb, UNIVORE. <My> aa).and(My.class)");

		Code.Linker binder =
				new Code.Linker(
						ImmutableMap.of("My", "xx.My"),
						ImmutableSet.of("aa", "HERBIVORE"));

		String joined = Code.join(binder.bind(terms));

		check(joined).is("this.@@aa(OPA.aa, CARNIVORE./* */aa, @@HERBIVORE. bb, UNIVORE. <xx.My> aa).and(xx.My.class)");
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

		check(Code.join(m1.getExpression("A.f"))).hasToString(" 1   +2;");
		check(Code.join(m1.getBlock("A.h"))).hasToString("{ return \"xxx\"; }");
		check(Code.join(m1.getExpression("A.B.u"))).hasToString("1;");
	}
}
