package org.immutables.fixture.style;

import org.junit.jupiter.api.Test;

public class JakartaTest {
	@Test
	public void haveAnnotation() {
		assert ImmutableLostInJakarta.class.isAnnotationPresent(
				jakarta.annotation.ParametersAreNonnullByDefault.class);
	}
}
