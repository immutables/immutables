package org.immutables.mongo.fixture.ent;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import org.immutables.gson.Gson.TypeAdapters;
import org.immutables.mongo.Mongo.Repository;
import org.immutables.mongo.fixture.ent.EntRepository.Criteria;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.mongo.types.TimeInstant;
import org.immutables.value.Value.Immutable;

@Immutable
@Repository
@TypeAdapters
public abstract class Ent {
  public abstract String uuid();

  public abstract String action();

  public abstract Optional<TimeInstant> expires();

  public static void main(String... args) throws UnknownHostException {
    MongoClient client = new MongoClient("localhost");
    RepositorySetup setup = RepositorySetup.builder()
        .database(client.getDB("test"))
        .executor(MoreExecutors.listeningDecorator(Executors.newCachedThreadPool()))
        .gson(new GsonBuilder()
            .registerTypeAdapterFactory(new GsonAdaptersEnt())
            .create())
        .build();

    EntRepository repository = new EntRepository(setup);

    EntRepository.Criteria where = repository.criteria()
        .uuid("8b7a881c-6ccb-4ada-8f6a-60cc99e6aa20")
        .actionIn("BAN", "IPBAN");

    Criteria or = where.expiresAbsent()
        .or().with(where).expiresGreaterThan(TimeInstant.of(1467364749679L));

    System.out.println(or);

    repository.find(or).fetchAll().getUnchecked();
  }
}
