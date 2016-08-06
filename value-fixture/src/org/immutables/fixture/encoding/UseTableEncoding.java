package org.immutables.fixture.encoding;

import org.immutables.value.Value;
import com.google.common.collect.Table;
import org.immutables.encode.fixture.TableEncodingEnabled;

@Value.Immutable
@TableEncodingEnabled
public abstract class UseTableEncoding<T> {

  abstract Table<String, Integer, T> getIt();

}
