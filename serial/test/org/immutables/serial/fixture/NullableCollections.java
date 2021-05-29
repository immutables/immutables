package org.immutables.serial.fixture;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Immutable
@Serial.Structural
public interface NullableCollections {
  @Nullable
  Collection<String> collection();

  @Nullable
  Set<String> set();

  @Nullable
  List<String> list();

  @Nullable
  Map<String, Integer> map();
}
