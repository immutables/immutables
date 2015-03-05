package org.immutables.mongo.fixture;

import org.immutables.gson.Gson;
import java.util.List;
import java.util.Set;
import org.immutables.mongo.Mongo;
import org.immutables.mongo.types.Binary;
import org.immutables.mongo.types.Id;
import org.immutables.value.Value;

@Mongo.Repository
@Value.Immutable
@Gson.TypeAdapters(emptyAsNulls = true)
public interface Item {

  @Mongo.Id
  String id();

  List<String> list();

  Set<Id> ids();

  Binary binary();
}
