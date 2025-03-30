package org.immutables.fixture.j17;

import org.immutables.value.Value.Builder;
import static org.immutables.value.Value.Default;

@Builder
public record RecordDefaults(
		@Default.Int(42) int a,
		@Default.String("ABC") String b,
		@Default.Long(100) long l,
		@Default.Long(-100) Long ll,
		String required) {}
