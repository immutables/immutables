package org.immutables.fixture.nullable;

import java.util.Map;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface FromNullCollection {
  @Nullable
  List<String> getItems();

  @Nullable
  Map<Integer, String> getFreq();
}
