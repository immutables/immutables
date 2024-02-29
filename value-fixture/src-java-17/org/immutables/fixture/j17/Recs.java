package org.immutables.fixture.j17;

import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Enclosing
public sealed interface Recs {
	@Builder record One(int aaa) implements Recs {}

	@Builder record Two(String bbb) implements Recs {}
}
