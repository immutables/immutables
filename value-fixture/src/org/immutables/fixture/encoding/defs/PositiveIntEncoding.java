package org.immutables.fixture.encoding.defs;

import org.immutables.encode.Encoding.Impl;
import org.immutables.encode.Encoding;

@Encoding
final class PositiveIntEncoding {
  @Encoding.Impl
  private int field;

  @Encoding.Of
  static int from(int v) {
    return v >= 0 ? v : -v;
  }

  public boolean equals(PositiveIntEncoding obj) {
    return field == obj.field;
  }

  @Override
  public int hashCode() {
    return field;
  }

  @Override
  public String toString() {
    return String.valueOf(field);
  }
}
