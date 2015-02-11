package org.immutables.mongo.fixture;

import org.bson.types.Binary;
import org.immutables.mongo.types.Id;
import java.util.Set;
import org.immutables.value.Value;
import org.immutables.gson.Gson;
import java.util.List;
import org.immutables.mongo.Mongo;

@Mongo.Repository
@Value.Immutable
@Gson.TypeAdapters
public interface Item {

  @Mongo.Id
  String id();

  List<String> list();

  Set<Id> ids();

  Binary binary();
}
