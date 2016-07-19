package org.immutables.value.processor.encode;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Set;
import org.immutables.generator.Naming;
import org.immutables.value.Value;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.encode.Code.Term;

@Immutable
@Enclosing
abstract class EncodedElement {
	abstract String name();
	abstract Type type();
	abstract Naming naming();
	abstract List<Param> params();
	abstract List<TypeParam> typeParams();
	abstract List<Term> code();
	abstract List<Type> thrown();
	abstract Set<String> tags();

	static class Builder extends ImmutableEncodedElement.Builder {}

	@Immutable
	abstract static class Param {
		@Value.Parameter
		abstract String name();
		@Value.Parameter
		abstract Type type();

		@Override
		public String toString() {
			return name() + ": " + type();
		}

		static Param of(String name, Type type) {
			return ImmutableEncodedElement.Param.of(name, type);
		}
	}

	@Immutable
	abstract static class TypeParam {
		abstract String name();
		abstract List<Type> bounds();

		@Override
		public String toString() {
			return name() + ": " + Joiner.on(" & ").join(bounds());
		}

		static class Builder extends ImmutableEncodedElement.TypeParam.Builder {}
	}
}
