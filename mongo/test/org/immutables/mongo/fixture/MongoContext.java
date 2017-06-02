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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mongodb.DB;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.immutables.mongo.fixture.holder.Holder;
import org.immutables.mongo.fixture.holder.HolderJsonSerializer;
import org.immutables.mongo.fixture.holder.ImmutableHolder;
import org.immutables.mongo.repository.RepositorySetup;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule which allows tests to access {@link RepositorySetup} and in-memory database (fongo).
 */
public class MongoContext extends ExternalResource {

  private final RepositorySetup setup;
  private final DB database;
  private final ListeningExecutorService executor;

  public MongoContext() {
    this.database = new Fongo("FakeMongo").getDB("testDB");
    this.executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    this.setup = RepositorySetup.builder()
        .gson(createGson())
        .executor(executor)
        .database(database)
        .build();
  }

  public DB database() {
    return database;
  }

  public RepositorySetup setup() {
    return setup;
  }

  private static com.google.gson.Gson createGson() {
    GsonBuilder gson = new GsonBuilder();
    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gson.registerTypeAdapterFactory(factory);
    }

    // register custom serializer for polymorphic Holder
    final HolderJsonSerializer custom = new HolderJsonSerializer();
    gson.registerTypeAdapter(Holder.class, custom);
    gson.registerTypeAdapter(ImmutableHolder.class, custom);

    return gson.create();
  }

  /**
   * Cleanup (terminate executor gracefully)
   */
  @Override
  protected void after() {
    MoreExecutors.shutdownAndAwaitTermination(executor, 100, TimeUnit.MILLISECONDS);
  }
}
