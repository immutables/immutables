/*
   Copyright 2013-2018 Immutables Authors and Contributors

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

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.mongo.bson4gson.GsonCodecs;
import org.immutables.mongo.repository.Repositories.Repository;
import org.immutables.mongo.types.TypeAdapters;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;


/**
 * {@link RepositorySetup} combines driver's database, thread-pool and serialization configuration
 * ({@link CodecRegistry}) to configure repositories extended from {@link Repositories.Repository}. Setup can
 * and usually should be shared between repositories which accesses the same database and share
 * other resources.
 * @see Repository
 */
@ThreadSafe
public final class RepositorySetup {

  final ListeningExecutorService executor;
  final CodecRegistry codecRegistry;
  final MongoDatabase database;
  final FieldNamingStrategy fieldNamingStrategy;

  private RepositorySetup(ListeningExecutorService executor, MongoDatabase database,
                          CodecRegistry codecRegistry,
                          FieldNamingStrategy fieldNamingStrategy) {
    this.executor = executor;
    this.database = database;
    this.codecRegistry = codecRegistry;
    this.fieldNamingStrategy = Preconditions.checkNotNull(fieldNamingStrategy, "fieldNamingStrategy");
  }

  /**
   * <p>Customize how fields are <strong>(re)</strong>named in mongo queries.
   *
   * <p>When building criterias (eg. by using {@link org.immutables.mongo.repository.Repositories.Finder}) immutables
   * doesn't have runtime information about final object format in the data-store
   * (serialization is delegated to {@link CodecRegistry}). Therefore, it is important that fields in the
   * query and mongo database match. This interface allows to hook codec logic into query construction so
   * immutables is aware of correct naming.
   *
   * <h3>Example</h3>
   * <p>Below is an example for <a href="https://github.com/google/gson">gson</a> which is
   * using <a href="https://google.github.io/gson/apidocs/com/google/gson/annotations/SerializedName.html">@SerializedName</a>
   * to control member naming.
   * <pre>
   * {@code
   *  final Gson gson = ...
   *  FieldNamingStrategy gsonStrategy = new FieldNamingStrategy() {
   *       @Override
   *       public String translateName(Member member) {
   *         return gson.fieldNamingStrategy().translateName((Field) member);
   *       }
   *  };
   * }
   * </pre>
   *
   * <p>Don't forget to employ same codec(s) for POJO serialization and current naming strategy.
   */
  public interface FieldNamingStrategy {

    /**
     * Provide a name for this {@code member} when used in queries.
     *
     * @param member current property (can be field / method) which is being translated
     * @return name of the property when serialized
     */
    String translateName(Member member);

    /**
     * Default implementation which uses same name of the property in queries.
     */
    FieldNamingStrategy DEFAULT = new FieldNamingStrategy() {
      @Override
      public String translateName(Member member) {
        return member.getName();
      }
    };

    /**
     * Uses {@link Gson#fieldNamingStrategy()} to translate names.
     */
    class GsonNamingStrategy implements FieldNamingStrategy {
      private final Gson gson;

      private GsonNamingStrategy(Gson gson) {
        this.gson = Preconditions.checkNotNull(gson, "gson");
      }

      @Override
      public String translateName(Member member) {
        return gson.fieldNamingStrategy().translateName((Field) member);
      }
    }
  }

  /**
   * Builder for {@link RepositorySetup}.
   * @see Builder#database(MongoDatabase)
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
    private MongoDatabase database;

    @Nullable
    private CodecRegistry codecRegistry;

    @Nullable
    private FieldNamingStrategy fieldNamingStrategy;

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
     * Configures repository to lookup {@link com.mongodb.client.MongoCollection collection} from the specified
     * {@code database} handle. Repository will inherit {@link WriteConcern} and
     * {@link ReadPreference} settings that was configured on supplied instance.
     * @param database database handle.
     * @return {@code this}
     * @see MongoClient#getDB(String)
     */
    public Builder database(MongoDatabase database) {
      this.database = checkNotNull(database);
      return this;
    }

    /**
     * Configures current repository setup with {@link Gson} serialization.
     *
     * <p>
     * You could use service providers for {@link TypeAdapterFactory} interface to register type
     * adapters. This mechanism, while optional, used a lot in immutables and allows for easy
     * registration if launched in a simple flat classpath.
     * 
     * <pre>
     * GsonBuilder gsonBuilder = new GsonBuilder();
     * gsonBuilder.registerTypeAdapterFactory(new TypeAdapters()); // BSON specific type adapters
     * ...
     * for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory)) {
     *   gsonBuilder.registerTypeAdapterFactory(factory);
     * }
     * Gson gson = gsonBuilder.create();
     * </pre>
     * 
     * The factory registration shown above is done by default when using
     * {@link RepositorySetup#forUri(String)} to create setup.
     * @param gson configured {@link Gson} instance
     * @see #codecRegistry(CodecRegistry) for other serialization options
     * @return {@code this}
     */
    public Builder gson(Gson gson) {
      checkNotNull(gson, "gson");

      // Will be used as a factory for BSON types (if Gson does not have one). By default, uses
      // TypeAdapter(s) from Gson if they're explicitly defined (not a ReflectiveTypeAdapter).
      // Otherwise delegate to BSON codec.
      TypeAdapterFactory bsonAdapterFactory = GsonCodecs.delegatingTypeAdapterFactory(
              MongoClient.getDefaultCodecRegistry()
      );

      // Appending new TypeAdapterFactory to allow Gson and Bson adapters to co-exists.
      // Depending on the type we may need to use one or another. For instance,
      // Date should be serialized by Gson (even if Bson has codec for it).
      // But ObjectId / Decimal128 by BSON (if Gson doesn't have a type adapter for it).
      // Document or BsonDocument should only be handled by BSON (it's unlikely that users have direct dependency on them in POJOs).
      // So newGson is a way to extend existing Gson instance with "BSON TypeAdapter(s)"
      Gson newGson = gson.newBuilder()
              .registerTypeAdapterFactory(bsonAdapterFactory)
              .create();

      // expose new Gson as CodecRegistry. Using fromRegistries() for caching
      CodecRegistry codecRegistry = CodecRegistries.fromRegistries(GsonCodecs.codecRegistryFromGson(newGson));

      return codecRegistry(codecRegistry, new FieldNamingStrategy.GsonNamingStrategy(gson));
    }

    /**
     * Register custom codec registry to serialize and deserialize entities using
     *  <a href="http://mongodb.github.io/mongo-java-driver/3.8/bson/codecs/">BSON</a>.
     *
     * <p>Also allows user to register custom naming strategy if POJOs have different property names
     * when serialized (important for correct querying).
     *
     * @param registry existing registry
     * @param fieldNamingStrategy allows to customize property names in queries
     * @return {@code this} same instance of the builder
     * @throws NullPointerException if one of arguments is null
     */
    public Builder codecRegistry(CodecRegistry registry, FieldNamingStrategy fieldNamingStrategy) {
      this.codecRegistry = checkNotNull(registry, "registry");
      this.fieldNamingStrategy = checkNotNull(fieldNamingStrategy, "fieldNamingStrategy");
      return this;
    }

    /**
     * Register custom codec registry to serialize and deserialize entities using
     * <a href="http://mongodb.github.io/mongo-java-driver/3.8/bson/codecs/">BSON</a>.
     *
     * <p>Please note that if you employ custom property naming (eg {@code @PropertyName} for
     * <a href="https://github.com/FasterXML/jackson">jackson</a>
     * or {@code @SerializedName} for <a href="https://github.com/google/gson">gson</a>) you need
     * to register {@link FieldNamingStrategy} otherwise mongo queries might not work correctly. Use
     * {@link #codecRegistry(CodecRegistry, FieldNamingStrategy)} for that. For additional information
     * consult documentation on {@link FieldNamingStrategy}.
     *
     * @param registry set of codecs to be used for serialization
     * @return {@code this} same instance of the builder
     * @throws NullPointerException if one of arguments is null
     */
    public Builder codecRegistry(CodecRegistry registry) {
      return codecRegistry(registry, FieldNamingStrategy.DEFAULT);
    }

    /**
     * Builds unmodifiable instance of {@link RepositorySetup}.
     * @return repository setup instance.
     */
    public RepositorySetup build() {
      checkState(executor != null, "executor is not set");
      checkState(database != null, "database is not set");
      checkState(codecRegistry != null, "codecRegistry is not set");
      checkState(fieldNamingStrategy != null, "fieldNamingStrategy is not set");

      return new RepositorySetup(executor, database, codecRegistry, fieldNamingStrategy);
    }
  }

  /**
   * Create setup using MongoDB client uri.
   * <ul>
   * <li>URI should contain database path segment</li>
   * <li>New internal {@link MongoClient} will be created</li>
   * <li>New internal executor will be created (with shutdown on jvm exit)</li>
   * <li>New {@link Gson} instance will be created configured with type adapter factory
   * providers</li>
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
        .database(newMongoClient(clientUri).getDatabase(databaseName))
        .executor(newExecutor())
        .gson(createGson())
        .build();
  }

  private static Gson createGson() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    // there are no longer auto-registed from class-path, but from here or if added manually.
    gsonBuilder.registerTypeAdapterFactory(new TypeAdapters());

    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gsonBuilder.registerTypeAdapterFactory(factory);
    }
    return gsonBuilder.create();
  }

  private static MongoClient newMongoClient(MongoClientURI clientUri) {
      return new MongoClient(clientUri);
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
