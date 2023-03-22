package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Style(jdkOnly = true)
@Value.Immutable
public interface HasFirstRedactedCompletely {
	@Value.Redacted
	int a1();
	int a2();
}
