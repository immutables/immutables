package org.immutables.fixture.encoding.defs;

import java.util.ArrayList;
import java.util.List;
import org.immutables.encode.Encoding;

@Encoding
final class MutableListEncoding<T> {
  @Encoding.Impl
  private List<T> impl;

  MutableListEncoding() {}

  @Encoding.Builder
  static final class Builder<T> {
    private List<T> list = new ArrayList<>();

    @Encoding.Naming(value = "set*")
    @Encoding.Init
    void setValue(T obj) {
      list.add(obj);
    }

    @Encoding.Init
    @Encoding.Copy
    void set(List<T> list) {
      this.list = list;
    }

    @Encoding.Build
    List<T> build() {
      return this.list;
    }
  }
}
