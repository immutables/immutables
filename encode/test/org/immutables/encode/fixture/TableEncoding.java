package org.immutables.encode.fixture;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.io.Serializable;
import org.immutables.encode.Encoding;

@Encoding
class TableEncoding<R, C, V> {

  @Encoding.Impl
  ImmutableTable<R, C, V> value = ImmutableTable.of();

  @Encoding.Expose
  Table<R, C, V> accessor() {
    return value;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  boolean equals(TableEncoding<R, C, V> other) {
    return value.equals(other.value);
  }

  private static <V extends Object & Serializable> V helper(V param) throws java.lang.Exception, java.lang.Error {
    return param;
  }

  @Encoding.Init
  static <R, C, V> ImmutableTable<R, C, V> init(Table<? extends R, ? extends C, ? extends V> table) {
    return ImmutableTable.copyOf(table);
  }

  @Encoding.Copy
  @Encoding.Naming("with*Put")
  Table<R, C, V> withPut(R row, C column, V value) {
    return ImmutableTable.<R, C, V>builder()
        .put(row, column, value)
        .build();
  }

  @Encoding.Naming("*CellSet")
  ImmutableSet<Table.Cell<R, C, V>> cellSet() {
    return value.cellSet();
  }

  @Encoding.Builder
  static class Builder<R, C, V> {
    ImmutableTable.Builder<R, C, V> builder = ImmutableTable.<R, C, V>builder();

    @Encoding.Naming("put*")
    @Encoding.Init
    Builder<R, C, V> put(R row, C column, V value) {
      builder.put(row, column, value);
      return this;
    }

    @Encoding.Naming("putAll*")
    @Encoding.Init
    Builder<R, C, V> putAll(Table<R, C, V> table) {
      builder.putAll(table);
      return this;
    }

    @Encoding.Build
    ImmutableTable<R, C, V> build() {
      return builder.build();
    }
  }
}
