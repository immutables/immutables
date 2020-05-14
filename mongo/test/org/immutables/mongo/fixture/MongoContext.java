/*
   Copyright 2017 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.mongo.fixture;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.io.Closer;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.immutables.mongo.fixture.holder.Holder;
import org.immutables.mongo.fixture.holder.HolderJsonSerializer;
import org.immutables.mongo.fixture.holder.ImmutableHolder;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.mongo.types.TypeAdapters;
import org.junit.rules.ExternalResource;

import java.io.Closeable;
import java.io.IOException;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * JUnit rule which allows tests to access {@link RepositorySetup} backed by real database (embedded but fake or remote MongoDB). It
 * is a good habit to run tests on different versions of the database. By default embedded (in memory) java server
 * is used.
 *
 * <p>If you want to connect to external mongo database use system property {@code mongo}.
 * With maven it will look something like this:
 * <pre>
 * {@code $ mvn test -DargLine="-Dmongo=mongodb://localhost"}
 * </pre>
 *
 * @see <a href="https://github.com/bwaldvogel/mongo-java-server">Mongo Java Server</a>
 **/
public class MongoContext extends ExternalResource implements AutoCloseable  {

  private static final String DBNAME = "testDB";

  private final Closer closer;
  private final RepositorySetup setup;
  private final MongoDatabase database;

  private MongoContext(final MongoClient client, Closer closer) {
    Preconditions.checkNotNull(client, "client");
    Preconditions.checkNotNull(closer, "closer");

    closer.register(client::close);

    this.database = client.getDatabase(DBNAME);
    clearDatabase(database);

    closer.register(() -> clearDatabase(database));

    final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    closer.register(() -> MoreExecutors.shutdownAndAwaitTermination(executor, 100, TimeUnit.MILLISECONDS));

    this.setup = RepositorySetup.builder()
            .gson(createGson())
            .executor(executor)
            .database(database)
            .build();

    this.closer = closer;
  }

  /**
   * Drops all collections from a mongo database
   */
  private static void clearDatabase(MongoDatabase database) {
    for (String name: database.listCollectionNames()) {
      database.getCollection(name).drop();
    }
  }

  public MongoDatabase database() {
    return database;
  }

  public RepositorySetup setup() {
    return setup;
  }

  private static com.google.gson.Gson createGson() {
    GsonBuilder gson = new GsonBuilder();

    // this one is no longer auto-registered
    gson.registerTypeAdapterFactory(new TypeAdapters());

    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gson.registerTypeAdapterFactory(factory);
    }

    // register custom serializer for polymorphic Holder
    final HolderJsonSerializer custom = new HolderJsonSerializer();
    gson.registerTypeAdapter(Holder.class, custom);
    gson.registerTypeAdapter(ImmutableHolder.class, custom);

    return gson.create();
  }

  public static MongoContext create() {
    final String uri = System.getProperty("mongo");
    final Closer closer = Closer.create();
    if (uri != null) {
      // remote mongo server
      return new MongoContext(new MongoClient(new MongoClientURI(uri)), closer);
    }

    final MongoServer server = new MongoServer(new MemoryBackend());
    closer.register(new Closeable() {
      @Override
      public void close() throws IOException {
        server.shutdownNow();
      }
    });

    final MongoClient client = new MongoClient(new ServerAddress(server.bind()));
    final MongoContext context = new MongoContext(client, closer);
    return context;
  }

  /**
   * Cleanup (terminate executor gracefully)
   */
  @Override
  protected void after() {
    try {
      close();
    } catch (Exception e) {
      Throwables.propagateIfPossible(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    closer.close();
  }
}
