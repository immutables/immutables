package org.immutables.fixture.modifiable;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
public interface NullableListMap {
  @Nullable
  Object getObject();

  @Nullable
  List<Object> getObjects();

  @Nullable
  Map<String, String> getMap();
}
