package org.immutables.fixture;

import java.util.List;
import java.util.Set;
import org.immutables.value.Value;

@SuppressWarnings({"all", "varargs"})
@Value.Immutable
public interface CopySuppressWarnings {
  Set<List<String>> elements();
}
