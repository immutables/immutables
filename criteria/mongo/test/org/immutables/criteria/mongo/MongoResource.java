/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.mongo;

import com.google.common.base.Throwables;
import com.google.common.io.Closer;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.reactivex.Flowable;
import org.junit.rules.ExternalResource;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * JUnit rule which allows to test repository access backed by real database (embedded but fake or remote MongoDB). It
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
public class MongoResource extends ExternalResource implements AutoCloseable  {


  private static final String DBNAME = "testDB";

  private final Closer closer;

  private final MongoDatabase database;

  private MongoResource(final MongoClient client, Closer closer) {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(closer, "closer");
    closer.register(client);

    // drop database if exists (to have a clean test)
    if (Flowable.fromPublisher(client.listDatabaseNames()).toList().blockingGet().contains(DBNAME)) {
      Flowable.fromPublisher(client.getDatabase(DBNAME).drop()).blockingFirst();
    }

    this.database = client.getDatabase(DBNAME);

    closer.register(database::drop);

    this.closer = closer;
  }

  public MongoDatabase database() {
    return database;
  }

  public static MongoResource create() {
    final String uri = System.getProperty("mongo");
    final Closer closer = Closer.create();

    if (uri != null) {
      // remote mongo server
      return new MongoResource(MongoClients.create(uri), closer);
    }

    final MongoServer server = new MongoServer(new MemoryBackend());
    final InetSocketAddress address = server.bind();

    closer.register(new Closeable() {
      @Override
      public void close() throws IOException {
        server.shutdownNow();
      }
    });

    final MongoClient client = MongoClients.create(String.format("mongodb://127.0.0.1:%d", address.getPort()));
    return new MongoResource(client, closer);
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
