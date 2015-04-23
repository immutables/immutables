package org.immutables.mongo.fixture;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import org.immutables.mongo.Mongo;

@Mongo.Repository
@Value.Immutable
@Gson.TypeAdapters
@Value.Style(
    typeAbstract = "*Val",
    typeImmutable = "*",
    visibility = Value.Style.ImplementationVisibility.PUBLIC)
interface GatenVal {

  @Mongo.Id
  String id();
}
