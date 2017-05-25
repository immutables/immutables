package org.immutables.mongo.fixture;

import com.github.fakemongo.Fongo;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mongodb.DB;
import org.immutables.mongo.fixture.holder.Holder;
import org.immutables.mongo.fixture.holder.HolderJsonSerializer;
import org.immutables.mongo.fixture.holder.ImmutableHolder;
import org.immutables.mongo.repository.RepositorySetup;
import org.junit.rules.ExternalResource;

import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
