package org.immutables.fixture.encoding;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.fixture.encoding.defs.MutableListEncodingEnabled;
import org.immutables.value.Value;

@JsonSerialize
@Value.Immutable
@MutableListEncodingEnabled
public interface FooMutableList {
  List<String> getInnerFoo();
}
