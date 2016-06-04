package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@JsonDeserialize(as = ImmutableNamingStrategy.class)
@Value.Immutable
@Value.Style(forceJacksonPropertyNames = false)
public interface NamingStrategy {
  int abraCadabra();

  boolean focusPocus();
}
