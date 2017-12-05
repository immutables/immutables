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

import com.github.fakemongo.Fongo;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.io.Closer;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import java.io.Closeable;
import java.io.IOException;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.immutables.mongo.fixture.holder.Holder;
import org.immutables.mongo.fixture.holder.HolderJsonSerializer;
import org.immutables.mongo.fixture.holder.ImmutableHolder;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.mongo.types.TypeAdapters;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule which allows tests to access {@link RepositorySetup} backed by real database (fongo or MongoDB). It
 * is a good habit to run tests on different versions of the database. By default Fongo is used.
 *
 * <p>If you want to connect to external mongo database use system property {@code mongo}.
 * With maven it will look something like this:
 * <pre>
 * {@code $ mvn test -DargLine="-Dmongo=mongodb://localhost"}
 * </pre>
 *
 * @see <a href="https://github.com/fakemongo/fongo">Fongo</a>
 **/
public class MongoContext extends ExternalResource implements AutoCloseable  {

  private static final String DBNAME = "testDB";

  private final Closer closer;
  private final RepositorySetup setup;
  private final MongoDatabase database;

  private MongoContext(final MongoClient client) {
    Preconditions.checkNotNull(client, "client");

    // allows to cleanup resources after each test
    final Closer closer = Closer.create();

    closer.register(new Closeable() {
      @Override
      public void close() throws IOException {
        client.close();
      }
    });

    // drop database if exists (to have a clean test)
    if (Iterables.contains(client.listDatabaseNames(), DBNAME)) {
      client.getDatabase(DBNAME).drop();
    }

    this.database = client.getDatabase(DBNAME);

    closer.register(new Closeable() {
      @Override
      public void close() throws IOException {
        database.drop();
      }
    });

    final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    closer.register(new Closeable() {
      @Override
      public void close() throws IOException {
        MoreExecutors.shutdownAndAwaitTermination(executor, 100, TimeUnit.MILLISECONDS);
      }
    });

    this.setup = RepositorySetup.builder()
            .gson(createGson())
            .executor(executor)
            .database(database)
            .build();

    this.closer = closer;
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
      return new MongoContext(createClient());
  }

  /**
   * Allows to switch between Fongo and MongoDB based on system parameter {@code mongo}.
   */
  private static MongoClient createClient() {
    String uri = System.getProperty("mongo");
    return uri != null
        ? new MongoClient(new MongoClientURI(uri))
        : new Fongo("FakeMongo").getMongo();
  }

  /**
   * Cleanup (terminate executor gracefully)
   */
  @Override
  protected void after() {
    try {
      close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    closer.close();
  }
}
