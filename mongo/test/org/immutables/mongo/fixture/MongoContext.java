package org.immutables.mongo.fixture;

import com.github.fakemongo.Fongo;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mongodb.DB;
import org.immutables.mongo.repository.RepositorySetup;
import org.junit.rules.ExternalResource;

import java.util.ServiceLoader;
import java.util.concurrent.Executors;

/**
 * JUnit rule which allows tests to access {@link RepositorySetup} and in-memory database (fongo).
 */
public class MongoContext extends ExternalResource {

    private final RepositorySetup setup;
    private final DB database;

    public MongoContext() {
        this.database = new Fongo("FakeMongo").getDB("testDB");

        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

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
        GsonBuilder gsonBuilder = new GsonBuilder();
        for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
            gsonBuilder.registerTypeAdapterFactory(factory);
        }
        return gsonBuilder.create();
    }
}
