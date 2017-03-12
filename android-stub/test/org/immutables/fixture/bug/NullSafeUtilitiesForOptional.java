package org.immutables.fixture.bug;

import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
public interface NullSafeUtilitiesForOptional {
  Optional<String> opt();
}
