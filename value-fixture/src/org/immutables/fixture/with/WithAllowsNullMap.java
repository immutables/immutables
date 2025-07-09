package org.immutables.fixture.with;

import java.util.List;
import java.util.Map;
import org.immutables.fixture.modifiable.AllowNulls;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
public interface WithAllowsNullMap extends WithWithAllowsNullMap {
  @AllowNulls Map<String, Object> map();
  @AllowNulls List<Object> list();
}
