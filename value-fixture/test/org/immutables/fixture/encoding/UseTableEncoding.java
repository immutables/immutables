package org.immutables.fixture.encoding;

import com.google.common.collect.Table;
import org.immutables.fixture.encoding.defs.TableEncodingEnabled;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@TableEncodingEnabled
public abstract class UseTableEncoding {

  abstract Table<String, Integer, Void> getIt();

}
