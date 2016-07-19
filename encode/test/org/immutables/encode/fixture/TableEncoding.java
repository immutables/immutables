package org.immutables.encode.fixture;

import java.io.Serializable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.immutables.encode.Encoding;

@Encoding
class TableEncoding<R, C, V> {

	@Encoding.Impl
	ImmutableTable<R, C, V> value = ImmutableTable.of();

	@Encoding.Expose
	Table<R, C, V> accessor() {
		return value;
	}

	@Encoding.Stringify
	String stringify() {
		return value.toString();
	}

	private static <V extends Object & Serializable> V helper(V param) {
		return param;
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

	@Encoding.Naming("*CellSet")
	ImmutableSet<Table.Cell<R, C, V>> cellSet() {
		return value.cellSet();
	}

	@Encoding.Builder
	class Builder {
		ImmutableTable.Builder<R, C, V> builder = ImmutableTable.<R, C, V>builder();

		@Encoding.Naming("put*")
		@Encoding.Init
		Builder put(R row, C column, V value) {
			builder.put(row, column, value);
			return this;
		}

		@Encoding.Naming("putAll*")
		@Encoding.Init
		Builder putAll(Table<R, C, V> table) {
			builder.putAll(table);
			return this;
		}

		@Encoding.Build
		ImmutableTable<R, C, V> build() {
			return builder.build();
		}
	}
}
