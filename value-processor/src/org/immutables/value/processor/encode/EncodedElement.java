package org.immutables.value.processor.encode;

import java.util.List;
import org.immutables.generator.Naming;
import org.immutables.value.Value;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.encode.Code.Term;

@Immutable
@Enclosing
interface EncodedElement {
	String name();
	Type type();
	Naming naming();
	List<Param> params();
	List<Term> code();

	class Builder extends ImmutableEncodedElement.Builder {}

	@Immutable
	interface Param {
		@Value.Parameter
		String name();
		@Value.Parameter
		Type type();
	}
}
