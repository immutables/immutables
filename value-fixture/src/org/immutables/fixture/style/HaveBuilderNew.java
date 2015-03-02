package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
public interface HaveBuilderNew {
  int a();

  default void use() {
    new ImmutableHaveBuilderNew.Builder().build();
  }
}
