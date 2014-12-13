package org.immutables.fixture.style;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Value.Nested
@Value.Immutable.Include({Target.class, Retention.class})
public interface IncludeNestedTypes {}
