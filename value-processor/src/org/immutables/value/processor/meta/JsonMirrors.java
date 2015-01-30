package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class JsonMirrors {
  private JsonMirrors() {}

  @Mirror.Annotation("com.fasterxml.jackson.annotation.JsonProperty")
  public @interface JsonProperty {
    String value();
  }
}
