package org.immutables.builder.fixture.telescopic;

import javax.annotation.Nullable;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Enclosing
@Value.Style(stagedBuilder = true)
public class StagedFactory {
	@Builder.Factory
	public static String superstring(int theory, String reality, @Nullable Void evidence) {
		return theory + " != " + reality + ", " + evidence;
	}

	@Builder.Factory
	public static String hop(@Builder.Parameter String a, int b) {
		return a + b;
	}
}
