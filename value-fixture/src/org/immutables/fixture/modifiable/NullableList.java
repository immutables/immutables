package org.immutables.fixture.modifiable;

import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
public interface NullableList {
  @Nullable
  Object getObject();

  @Nullable
  List<Object> getObjects();
}
