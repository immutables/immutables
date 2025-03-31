package org.immutables.fixture.j17;

import java.lang.annotation.RetentionPolicy;
import javax.annotation.Nullable;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

// TODO , attributeBuilderDetection = true, deepImmutablesDetection = true
@Value.Style(typeImmutableEnclosing = "*s", jdkOnly = true)
@Value.Enclosing
public sealed interface Recs {
	@Value.Builder record One(int aaa) implements Recs, Recss.WithOne {}

	@Builder record Two(String bbb) implements Recs, Recss.WithTwo {}


	@Value.Builder record Three<T>(T bbb) implements Recs /*, Recss.WithThree<T> // This Doesn't work on CI??  */ {}

	@Value.Builder record FourSwitch(
			@Builder.Switch(defaultName = "SOURCE") RetentionPolicy policy) implements Recs, Recss.WithFourSwitch {}

	@Value.Builder
	record Another(One buildme, Two andme) {}

	// checking if default works in factory too
	@Builder.Factory
	static String factory(String left, @Value.Default.String("::") String operator, String right) {
		return left + operator + right;
	}

	@Value.Immutable
	interface RegularImm {
		@Value.Default.Boolean(true) boolean b();
		@Value.Default.Char('c') char c();
		@Value.Default.Boolean(true) Boolean bb();
		@Nullable @Value.Default.Char('K') Character cc();
	}
}
