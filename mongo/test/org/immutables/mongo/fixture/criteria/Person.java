package org.immutables.mongo.fixture.criteria;

import com.google.common.base.Optional;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;

import java.util.Date;
import java.util.List;

@Mongo.Repository
@Value.Immutable
@Gson.TypeAdapters
interface Person {

  @Mongo.Id
  String id();


  String name();

  int age();

  Optional<Date> dateOfBirth();

  List<String> aliases();

}
