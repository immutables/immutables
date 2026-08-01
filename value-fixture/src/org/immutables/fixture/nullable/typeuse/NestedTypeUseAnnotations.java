package org.immutables.fixture.nullable.typeuse;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;
import org.jspecify.annotations.Nullable;

/**
 * Reproducer for TYPE_USE annotations nested in generics / wildcard bounds (#1667).
 */
@Value.Immutable
@Value.Style(jdkOnly = true)
public interface NestedTypeUseAnnotations {
  List<Map.Entry<String, ? extends @Nullable Object>> bars();

  List<@Nullable String> nullableElements();

  Map<String, @Nullable Serializable> annotatedValue();
}
