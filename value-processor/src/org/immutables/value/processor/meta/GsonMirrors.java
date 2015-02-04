package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class GsonMirrors {
  private GsonMirrors() {}

  @Mirror.Annotation("org.immutables.gson.Gson.TypeAdapted")
  public @interface TypeAdapted {}

  @Mirror.Annotation("org.immutables.gson.Gson.Subtypes")
  public @interface Subtypes {
    Class<?>[] value();
  }

  @Mirror.Annotation("org.immutables.gson.Gson.Named")
  public @interface Named {
    String value();
  }

  @Mirror.Annotation("org.immutables.gson.Gson.Ignore")
  public @interface Ignore {}
}
