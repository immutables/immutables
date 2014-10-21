package org.immutables.fixture;

import org.immutables.mongo.Mongo;
import org.immutables.common.repository.Id;
import org.immutables.json.Json;
import org.immutables.value.Value;

@Value.Immutable
@Mongo.Repository("ent2")
public abstract class SillyEntitySecond {

  @Json.Named("_id")
  @Value.Default
  public Id id() {
    return Id.generate();
  }
}
