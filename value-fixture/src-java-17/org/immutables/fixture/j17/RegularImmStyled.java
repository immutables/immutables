package org.immutables.fixture.j17;

import javax.annotation.Nullable;
import org.immutables.value.Value;

// Checking alternative generation Style
@Value.Style(unsafeDefaultAndDerived = true)
@Value.Modifiable
@Value.Immutable(singleton = true)
public interface RegularImmStyled {
	@Value.Default.Boolean(true) boolean b();
	@Value.Default.Char('c') char c();
	@Value.Default.Boolean(true) Boolean bb();
	@Nullable @Value.Default.Char('K') Character cc();
}
