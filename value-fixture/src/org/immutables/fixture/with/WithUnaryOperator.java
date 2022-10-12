package org.immutables.fixture.with;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(withUnaryOperator = "map*")
interface WithUnaryOperator {
	int a();
	String b();
	Optional<String> o();
	List<String> l();
	OptionalInt i();
	OptionalDouble d();
	com.google.common.base.Optional<Integer> oi();
}
