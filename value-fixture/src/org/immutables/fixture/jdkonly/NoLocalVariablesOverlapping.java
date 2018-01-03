package org.immutables.fixture.jdkonly;

import org.immutables.value.Value;
import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Style(jdkOnly = true)
public abstract class NoLocalVariablesOverlapping {
  public abstract Map<String, String> getEntry();

  public abstract Map<String, String> getK();

  public abstract Map<String, String> getV();

  public abstract List<String> getElements();
}
