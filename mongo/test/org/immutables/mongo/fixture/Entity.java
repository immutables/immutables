package org.immutables.mongo.fixture;

import com.google.common.base.Optional;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;

@Mongo.Repository
@Value.Immutable
@Gson.TypeAdapters
public abstract class Entity {

  @Mongo.Id
  abstract String id();

  @Value.Default
  public int version() {
    return 0;
  }

  abstract Optional<String> value();

}
