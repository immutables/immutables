package org.immutables.fixture.encoding;

import org.immutables.value.Value;
import org.immutables.encode.fixture.VoidEncodingEnabled;

@VoidEncodingEnabled
@Value.Immutable
public interface UseVoidEncoding {
  Void attr();
}
