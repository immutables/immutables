package org.immutables.fixture.generics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import java.util.List;
import org.immutables.value.Value;

// 
@Value.Immutable
@Value.Style(allParameters = true)
@JsonSerialize(as = ImmutableFoo.class)
@JsonDeserialize(as = ImmutableFoo.class)
public interface OptionalCastedInConstructor {
  Optional<List<Long>> getList();
}