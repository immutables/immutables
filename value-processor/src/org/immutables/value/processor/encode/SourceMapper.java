package org.immutables.value.processor.encode;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.immutables.value.processor.encode.Code.Term;
import org.immutables.value.processor.encode.Structurizer.Statement;

final class SourceMapper {
	private static final Statement EMPTY_STATEMENT = new Statement.Builder().build();

	final Map<String, Statement> definitions = new LinkedHashMap<>();
	final Function<String, Statement> get = Functions.forMap(definitions, EMPTY_STATEMENT);

	SourceMapper(CharSequence source) {
		List<Term> terms = Code.termsFrom(source.toString());
		mapDefinitions("", new Structurizer(terms).structurize());
	}

	private void mapDefinitions(String prefix, List<Statement> statements) {
		for (Statement statement : statements) {
			if (statement.isClass()) {
				mapDefinitions(prefix + statement.name().get() + ".", statement.definitions());
			} else if (statement.name().isPresent()) {
				definitions.put(prefix + statement.name().get(), statement);
			}
		}
	}

	List<Term> getExpression(String path) {
		return get.apply(path).expression();
	}

	List<Term> getBlock(String path) {
		return get.apply(path).block();
	}

	List<Term> getAnnotations(String path) {
		return get.apply(path).annotations();
	}
}
