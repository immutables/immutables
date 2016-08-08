package org.immutables.encode.fixture;

import org.immutables.encode.Encoding;

// testbed for synth element
@Encoding
class VoidEncoding {
  @Encoding.Impl
  private Void impl;
}
