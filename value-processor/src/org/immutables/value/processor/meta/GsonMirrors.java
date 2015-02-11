package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class GsonMirrors {
  private GsonMirrors() {}

  @Mirror.Annotation("org.immutables.gson.Gson.TypeAdapters")
  public @interface TypeAdapters {
    boolean fieldNamingStrategy() default false;

    boolean emptyAsNulls() default false;
  }

  @Mirror.Annotation("org.immutables.gson.Gson.ExpectedSubtypes")
  public @interface ExpectedSubtypes {
    Class<?>[] value() default {};
  }

  @Mirror.Annotation("org.immutables.gson.Gson.Named")
  public @interface Named {
    String value();
  }

  @Mirror.Annotation("org.immutables.gson.Gson.Ignore")
  public @interface Ignore {}
}
