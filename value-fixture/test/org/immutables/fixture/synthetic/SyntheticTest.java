package org.immutables.fixture.synthetic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SyntheticTest {

	@Test
	void testEquals() {
		Assertions.assertEquals(
			ImmutableSynthetic.builder()
				.synthetic(10)
				.build(),
			ImmutableSynthetic.builder()
				.synthetic(10)
				.build());
	}

}