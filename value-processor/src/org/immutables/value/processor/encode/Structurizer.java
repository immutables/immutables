package org.immutables.value.processor.encode;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Verify;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.ArrayList;
import java.util.List;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.encode.Code.Term;

@Enclosing
final class Structurizer {
	private final PeekingIterator<Term> terms;
	private final WhitespaceEnabler whitespaces = new WhitespaceEnabler();

	Structurizer(Iterable<Term> terms) {
		this.terms = Iterators.peekingIterator(Iterators.filter(terms.iterator(), whitespaces));
	}

	private final class WhitespaceEnabler implements Predicate<Term> {
		private int count;
		void on() {
			count++;
		}
		void off() {
			if (--count < 0) throw new IllegalStateException("unmatched off");
		}
		@Override
		public boolean apply(Term input) {
			return count > 0 || (!input.isWhitespace() && !input.isComment());
		}
	}

	@Immutable
	abstract static class Statement {
		abstract List<Term> annotations();
		abstract List<Term> signature();
		abstract List<Term> parameters();
		abstract List<Term> expression();
		abstract List<Term> block();
		abstract List<Statement> definitions();

		@Derived
		boolean isClass() {
			for (Term t : signature()) {
				if (t.is("class")) {
					return true;
				}
			}
			return false;
		}

		@Default
		Optional<String> name() {
			if (signature().isEmpty()) {
				return Optional.absent();
			}
			Term last = Iterables.getLast(signature());
			if (last.isWordOrNumber() && !last.is("static")) {
				return Optional.of(last.toString());
			}
			return Optional.absent();
		}

		static class Builder extends ImmutableStructurizer.Statement.Builder {}
	}

	List<Statement> structurize() {
		List<Statement> result = new ArrayList<>();
		while (terms.hasNext()) {
			result.add(statement());
		}
		return result;
	}

	private Statement statement() {
		Statement.Builder builder = new Statement.Builder();
		boolean classDecl = false;
		boolean wasParameters = false;
		for (;;) {
			Term t = terms.peek();
			if (t.is("=")) {
				terms.next();
				expressionUntilSemicolon(builder);
				return builder.build();
			} else if (t.is("(") && !wasParameters) {
				builder.addAllParameters(collectUntilMatching(")"));
				wasParameters = true;
			} else if (t.is("{")) {
				block(builder, classDecl);
				return builder.build();
			} else if (t.is(";")) {
				terms.next();
				return builder.build();
			} else {
				if (wasParameters) {
					terms.next();// just throwing away throws information
				} else {
					if (signature(builder)) {
						classDecl = true;
						// take class name as next token after class keyword
						builder.name(Optional.of(terms.peek().toString()));
					}
				}
			}
		}
	}

	private List<Term> collectUntilMatching(String end) {
		List<Term> result = new ArrayList<>();
		doCollectMatching(result, terms.peek().toString(), end);
		return result;
	}

	private void doCollectMatching(List<Term> accumulator, String start, String end) {
		whitespaces.on();
		try {
			accumulator.add(terms.next());

			for (;;) {
				Term t = terms.peek();
				if (t.is(start)) {
					doCollectMatching(accumulator, start, end);
				} else if (t.is(end)) {
					accumulator.add(terms.next());
					return;
				} else {
					accumulator.add(terms.next());
				}
			}
		} finally {
			whitespaces.off();
		}
	}

	private void expressionUntilSemicolon(Statement.Builder builder) {
		whitespaces.on();
		try {
			List<Term> result = new ArrayList<>();
			for (;;) {
				Term t = terms.peek();
				if (t.is("(")) {
					doCollectMatching(result, "(", ")");
				} else if (t.is("{")) {
					doCollectMatching(result, "{", "}");
				} else if (t.is("[")) {
					doCollectMatching(result, "[", "]");
				} else {
					result.add(terms.next());
					if (t.is(";")) {
						builder.addAllExpression(result);
						return;
					}
				}
			}
		} finally {
			whitespaces.off();
		}
	}

	private void block(Statement.Builder builder, boolean classDecl) {
		if (classDecl) {
			Verify.verify(terms.peek().is("{"));
			terms.next();
			while (terms.hasNext() && !terms.peek().is("}")) {
				builder.addDefinitions(statement());
			}
			Verify.verify(terms.next().is("}"));
		} else {
			builder.addAllBlock(collectUntilMatching("}"));
		}
	}

	private boolean signature(Statement.Builder builder) {
		Term t = terms.peek();
		if (t.is("@")) {
			do {
				builder.addAnnotations(terms.next());
				Verify.verify(terms.peek().isWordOrNumber());
				builder.addAnnotations(terms.next());
			} while (terms.peek().is("."));

			if (terms.peek().is("(")) {
				builder.addAllAnnotations(collectUntilMatching(")"));
			}
			return false;
		} else if (t.is("<")) {
			builder.addAllSignature(collectUntilMatching(">"));
			return false;
		} else if (t.is("class")) {
			builder.addSignature(terms.next());
			return true;
		} else {
			builder.addSignature(terms.next());
			return false;
		}
	}
}
