package org.immutables.mongo.types;

import org.bson.types.ObjectId;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.mongo.fixture.MongoContext;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

public class InternalTypesTest {
  @Rule
  public final MongoContext context = MongoContext.create();

  private final InternalTypesRepository repository = new InternalTypesRepository(context.setup());

  @Test
  public void readWrite() throws Exception {
    InternalTypes types = ImmutableInternalTypes.builder()
            .objectId(ObjectId.get())
            .id(Id.from(ObjectId.get().toByteArray()))
            .binary(Binary.create(new byte[] {1, 2, 3}))
            .time(TimeInstant.of(System.currentTimeMillis()))
            .build();

    repository.insert(types).getUnchecked();

    InternalTypes types2 = repository.findAll().fetchAll().getUnchecked().get(0);

    check(types2).is(types);
  }

  @Mongo.Repository
  @Value.Immutable
  @Gson.TypeAdapters
  interface InternalTypes {
    @Mongo.Id
    ObjectId objectId();

    Id id();

    Binary binary();

    TimeInstant time();
  }
}
