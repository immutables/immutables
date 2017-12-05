package org.immutables.fixture.generics;

import com.google.common.base.Optional;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(allParameters = true)
public interface OptionalCastedInConstructor {
  Optional<List<Long>> getList();
}