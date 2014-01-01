/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.common.repository;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.immutables.common.repository.Repositories.Repository;
import org.immutables.common.repository.internal.RepositorySetupExecutors;
import static com.google.common.base.Preconditions.*;

/**
 * {@link RepositorySetup} combines collaborating services and settings to configure generated
 * repositories (extended from {@link Repositories.Repository}).
 * @see Repository#Repository(RepositorySetup, String, org.immutables.common.marshal.Marshaler)
 */
@ThreadSafe
public final class RepositorySetup {

  final ListeningExecutorService executor;
  final DB database;

  private RepositorySetup(ListeningExecutorService executor, DB database) {
    this.executor = executor;
    this.database = database;
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
     * {@link ReadPreference} settings that was Fsconfigured on supplied instance.
     * @param database database handle.
     * @return {@code this}
     * @see MongoClient#getDB(String)
     */
    public Builder database(DB database) {
      this.database = checkNotNull(database);
      return this;
    }

    /**
     * Builds unmodifiable instance of {@link RepositorySetup}.
     * @return repository setup instance.
     */
    public RepositorySetup build() {
      checkState(executor != null, "executor is not set");
      checkState(database != null, "database is not set");
      return new RepositorySetup(executor, database);
    }
  }

  /**
   * Create setup using MongoDB client uri.
   * <ul>
   * <li>URI should contain database path segment</li>
   * <li>New internal {@link MongoClient} will be created</li>
   * <li>New internal executor will be created (with shutdown on jvm exit)</li>
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
    @Nullable
    String databaseName = clientUri.getDatabase();
    checkArgument(databaseName != null, "URI should contain database path segment");

    return builder()
        .database(newMongoClient(clientUri).getDB(databaseName))
        .executor(RepositorySetupExecutors.newExecutor())
        .build();
  }

  private static MongoClient newMongoClient(MongoClientURI clientUri) {
    try {
      return new MongoClient(clientUri);
    } catch (UnknownHostException ex) {
      throw Throwables.propagate(ex);
    }
  }
}
