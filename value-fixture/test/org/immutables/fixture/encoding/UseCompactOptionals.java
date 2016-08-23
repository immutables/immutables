package org.immutables.fixture.encoding;

import org.immutables.fixture.encoding.defs.PositiveIntEncodingEnabled;
import org.immutables.fixture.encoding.defs.CompactOptionalDoubleEnabled;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.immutables.fixture.encoding.defs.CompactOptionalIntEnabled;
import org.immutables.value.Value;

@Value.Style(builder = "newBuilder")
@Value.Immutable
@CompactOptionalIntEnabled
@CompactOptionalDoubleEnabled
@PositiveIntEncodingEnabled
public interface UseCompactOptionals {
  @Value.Parameter
  int abc();

  @Value.Parameter
  OptionalInt a();

  @Value.Parameter
  OptionalInt b();

  @Value.Parameter
  OptionalDouble c();

  @Value.Parameter
  OptionalDouble d();

  @Value.Parameter
  OptionalDouble builder();
}
