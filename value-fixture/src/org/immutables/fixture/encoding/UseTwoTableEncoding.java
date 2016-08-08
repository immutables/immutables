package org.immutables.fixture.encoding;

import com.google.common.collect.Table;
import org.immutables.encode.fixture.TableEncodingEnabled;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
@TableEncodingEnabled
public abstract class UseTwoTableEncoding<T, V> {
  abstract Table<V, Integer, T> it();

  abstract Table<T, V, String> wit();
}
