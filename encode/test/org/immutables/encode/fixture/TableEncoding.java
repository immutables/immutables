package org.immutables.encode.fixture;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.immutables.encode.Encoding;

@Encoding
class TableEncoding<R, C, V> {

  @Encoding.Impl
  ImmutableTable<R, C, V> value = ImmutableTable.of();

  @Encoding.Expose
  Table<R, C, V> value() {
    return value;
  }

  @Encoding.Stringify
  String stringify() {
    return value.toString();
  }

  @Encoding.Init
  ImmutableTable<R, C, V> init(Table<? extends R, ? extends C, ? extends V> table) {
    return ImmutableTable.copyOf(table);
  }

  @Encoding.Copy
  @Encoding.Naming("with*Put")
  Table<R, C, V> withPut(R row, C column, V value) {
    return ImmutableTable.<R, C, V>builder()
        .put(row, column, value)
        .build();
  }

  @Encoding.Derive
  @Encoding.Naming("*CellSet")
  ImmutableSet<Table.Cell<R, C, V>> cellSet() {
    return value.cellSet();
  }

  class Builder {
    ImmutableTable.Builder<R, C, V> builder = ImmutableTable.<R, C, V>builder();

    @Encoding.Naming("put*")
    Builder put(R row, C column, V value) {
      builder.put(row, column, value);
      return this;
    }

    @Encoding.Naming("putAll*")
    Builder putAll(Table<R, C, V> table) {
      builder.putAll(table);
      return this;
    }

    ImmutableTable<R, C, V> build() {
      return builder.build();
    }
  }
}
