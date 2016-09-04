package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(get = {"get*", "is*"}, init = "set*", forceJacksonPropertyNames = false)
@JsonDeserialize(as = ImmutableKeywordNames.class)
public interface KeywordNames {
  long getLong();

  boolean isDefault();

  default void use() {
    ImmutableKeywordNames names = ImmutableKeywordNames.builder()
        .setDefault(true)
        .setLong(1L)
        .build();

    names.getLong();
    names.isDefault();
  }
}
