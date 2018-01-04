package org.immutables.fixture.modifiable;

import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Style(jdkOnly = true)
@Value.Immutable
@Value.Modifiable
public interface AllowNullsObject {

  @AllowNulls
  Map<String, String> getMap();

  @AllowNulls
  List<String> getList();
}
