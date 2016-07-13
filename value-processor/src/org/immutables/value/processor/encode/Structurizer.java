package org.immutables.value.processor.encode;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import org.immutables.value.processor.encode.Code.Term;

final class Structurizer {
	private final PeekingIterator<Term> terms;

	Structurizer(Iterable<Term> terms) {
		this.terms = Iterators.peekingIterator(terms.iterator());
	}

	void structurize() {

		while (terms.hasNext()) {
			statement();
		}
	}

	private void statement() {
		while (terms.hasNext()) {
			Term t = terms.next();

		}
	}

	interface CompilationUnit {
		List<Statement> statements();
	}

	interface Statement {
		List<Term> body();
	}

	interface Elem {

	}

	interface Params {

	}

	interface Annotation extends Elem {

	}
}
