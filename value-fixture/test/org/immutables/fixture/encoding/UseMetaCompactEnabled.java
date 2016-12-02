package org.immutables.fixture.encoding;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
@MetaCompactEnabled
public interface UseMetaCompactEnabled {
  OptionalInt a();

  OptionalDouble b();
}
