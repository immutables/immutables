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

import com.google.common.io.Closer;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.reactivex.Flowable;

import java.net.InetSocketAddress;
import java.util.Objects;

class MongoInstance implements AutoCloseable {

  private static final String DBNAME = "testDB";

  private final Closer closer;

  private final MongoClient client;
  private final MongoDatabase database;

  private MongoInstance(MongoClient client) {
    this(client, Closer.create());
  }

  private MongoInstance(MongoClient client, Closer closer) {
    Objects.requireNonNull(closer, "closer");
    this.client = Objects.requireNonNull(client, "client");
    closer.register(client);

    // drop database if exists (to have a clean test)
    if (Flowable.fromPublisher(client.listDatabaseNames()).toList().blockingGet().contains(DBNAME)) {
      Flowable.fromPublisher(client.getDatabase(DBNAME).drop()).blockingFirst();
    }

    this.database = client.getDatabase(DBNAME);

    closer.register(database::drop);

    this.closer = closer;
  }

  MongoClient client() {
    return client;
  }

  MongoDatabase database() {
    return database;
  }

  static MongoInstance create() {
    final String uri = System.getProperty("mongo");


    if (uri != null) {
      // connect to remote mongo server
      return new MongoInstance(MongoClients.create(uri));
    }

    final MongoServer server = new MongoServer(new MemoryBackend());
    final InetSocketAddress address = server.bind();
    final Closer closer = Closer.create();
    closer.register(server::shutdownNow);

    final MongoClient client = MongoClients.create(String.format("mongodb://127.0.0.1:%d", address.getPort()));
    return new MongoInstance(client, closer);
  }

  @Override
  public void close() throws Exception {
    closer.close();
  }

}
