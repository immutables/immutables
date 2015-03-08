/*
    Copyright 2013-2015 Immutables Authors and Contributors

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
package org.immutables.mongo.repository;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import java.net.UnknownHostException;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.immutables.mongo.repository.Repositories.Repository;
import static com.google.common.base.Preconditions.*;

/**
 * {@link RepositorySetup} combines driver's database, thread-pool and serialization configuration
 * ({@link Gson}) to configure repositories extended from {@link Repositories.Repository}. Setup can
 * and usually should be shared between repositories which accesses the same database and share
 * other resources.
 * @see Repository
 */
@ThreadSafe
public final class RepositorySetup {

  final ListeningExecutorService executor;
  final Gson gson;
  final DB database;

  private RepositorySetup(ListeningExecutorService executor, DB database, Gson gson) {
    this.executor = executor;
    this.database = database;
    this.gson = gson;
  }

  /**
   * Builder for {@link RepositorySetup}.
   * @see Builder#database(DB)
   * @see Builder#executor(ListeningExecutorService)
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Configures and builds unmodifiable instance of {@link RepositorySetup}.
   */
  @NotThreadSafe
  public static class Builder {
    @Nullable
    private ListeningExecutorService executor;
    @Nullable
    private DB database;
    @Nullable
    private Gson gson;

    private Builder() {}

    /**
     * Configures repository to use specified executor for all operations.
     * @param executor executor that will be used by repositories.
     * @return {@code this}
     * @see Executors
     * @see MoreExecutors#listeningDecorator(java.util.concurrent.ExecutorService)
     */
    public Builder executor(ListeningExecutorService executor) {
      this.executor = checkNotNull(executor);
      return this;
    }

    /**
     * Configures repository to lookup {@link DBCollection collection} from the specified
     * {@code database} handle. Repository will inherit {@link WriteConcern} and
     * {@link ReadPreference} settings that was configured on supplied instance.
     * @param database database handle.
     * @return {@code this}
     * @see MongoClient#getDB(String)
     */
    public Builder database(DB database) {
      this.database = checkNotNull(database);
      return this;
    }

    /**
     * Configures {@link Gson} instance.
     * <p>
     * You could use service providers for {@link TypeAdapterFactory} interface to register type
     * adapters. This mechanism, while optional, used a lot in immutables and allows for easy
     * registration if launched in a simple flat classpath.
     * 
     * <pre>
     * GsonBuilder gsonBuilder = new GsonBuilder();
     * ...
     * for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory)) {
     *   gsonBuilder.
     * }
     * Gson gson = gsonBuilder.create();
     * </pre>
     * 
     * The factory registration shown above is done by default when using
     * {@link RepositorySetup#forUri(String)} to create setup.
     * @param gson configured {@link Gson} instance
     * @return {@code this}
     */
    public Builder gson(Gson gson) {
      this.gson = checkNotNull(gson);
      return this;
    }

    /**
     * Builds unmodifiable instance of {@link RepositorySetup}.
     * @return repository setup instance.
     */
    public RepositorySetup build() {
      checkState(executor != null, "executor is not set");
      checkState(database != null, "database is not set");
      checkState(gson != null, "gson is not set");
      return new RepositorySetup(executor, database, gson);
    }
  }

  /**
   * Create setup using MongoDB client uri.
   * <ul>
   * <li>URI should contain database path segment</li>
   * <li>New internal {@link MongoClient} will be created</li>
   * <li>New internal executor will be created (with shutdown on jvm exit)</li>
   * <li>New {@link Gson} instance will be created configured with type adapter factory providers</li>
   * </ul>
   * <p>
   * Setup created by this factory methods should be reused to configure collection repositories for
   * the same MongoDB database.
   * <p>
   * This constructor designed for ease of use in sample scenarious. For more flexibility consider
   * using {@link #builder()} with custom constructed {@link ListeningExecutorService
   * executor} and {@link DB database} handle.
   * @param uri string that will be parsed as {@link MongoClientURI}.
   * @see MongoClientURI
   * @return repository setup instance.
   */
  public static RepositorySetup forUri(String uri) {
    MongoClientURI clientUri = new MongoClientURI(uri);
    @Nullable String databaseName = clientUri.getDatabase();
    checkArgument(databaseName != null, "URI should contain database path segment");

    return builder()
        .database(newMongoClient(clientUri).getDB(databaseName))
        .executor(newExecutor())
        .gson(createGson())
        .build();
  }

  private static Gson createGson() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gsonBuilder.registerTypeAdapterFactory(factory);
    }
    return gsonBuilder.create();
  }

  private static MongoClient newMongoClient(MongoClientURI clientUri) {
    try {
      return new MongoClient(clientUri);
    } catch (UnknownHostException ex) {
      throw Throwables.propagate(ex);
    }
  }

  private static final int DEFAULT_THREAD_POOL_CORE_SIZE = 5;
  private static final int DEFAULT_THREAD_POOL_MAXIMUM_SIZE = 15;
  private static final long DEFAULT_THREAD_POOL_KEEP_ALIVE_MILLIS = TimeUnit.MINUTES.toMillis(1);

  private static final ThreadFactory DEFAULT_THREAD_FACTORY =
      new ThreadFactoryBuilder()
          .setNameFormat(RepositorySetup.class.getPackage().getName() + "-%s")
          .setDaemon(true)
          .build();

  private static ListeningExecutorService newExecutor() {
    return MoreExecutors.listeningDecorator(
        MoreExecutors.getExitingExecutorService(
            new ThreadPoolExecutor(
                DEFAULT_THREAD_POOL_CORE_SIZE,
                DEFAULT_THREAD_POOL_MAXIMUM_SIZE,
                DEFAULT_THREAD_POOL_KEEP_ALIVE_MILLIS,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                DEFAULT_THREAD_FACTORY)));
  }
}
