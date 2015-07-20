package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class OkJsonMirrors {
  private OkJsonMirrors() {}

  @Mirror.Annotation("org.immutables.moshi.Json.Adapters")
  public @interface OkTypeAdapters {
    boolean emptyAsNulls() default false;
  }

  @Mirror.Annotation("org.immutables.moshi.Json.Named")
  public @interface OkNamed {
    String value();
  }

  @Mirror.Annotation("org.immutables.moshi.Json.Ignore")
  public @interface OkIgnore {}
}
