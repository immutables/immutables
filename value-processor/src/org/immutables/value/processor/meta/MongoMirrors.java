package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class MongoMirrors {
  private MongoMirrors() {}

  @Mirror.Annotation("org.immutables.mongo.Mongo.Repository")
  public @interface Repository {
    String value() default "";
  }

  @Mirror.Annotation("org.immutables.mongo.Mongo.Id")
  public @interface Id {}
}
