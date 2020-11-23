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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.Flowable;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.criteria.backend.ContainerNaming;
import org.immutables.criteria.mongo.bson4jackson.BsonModule;
import org.immutables.criteria.mongo.bson4jackson.IdAnnotationModule;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;

import java.util.Objects;

/**
 * Creates mongo collection on demand. Usually in conjunction with {@link MongoExtension}.
 */
class BackendResource  {

  private final MongoDatabase database;

  private final MongoBackend backend;

  private final CodecRegistry registry;

  private final LazyResolver resolver;

  BackendResource(MongoDatabase database) {
    this.database = Objects.requireNonNull(database, "database");
    final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new BsonModule())
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module())
            .registerModule(new IdAnnotationModule());

    this.registry = JacksonCodecs.registryFromMapper(mapper);
    this.resolver = new LazyResolver();
    this.backend = new MongoBackend(MongoSetup.of(this.resolver));
  }

  MongoBackend backend() {
    return backend;
  }

  MongoDatabase database() {
    return database;
  }

  @SuppressWarnings("unchecked")
  <T> MongoCollection<T> collection(Class<T> entityType) {
    return (MongoCollection<T>) resolver.resolve(entityType);
  }

  private class LazyResolver implements CollectionResolver {

    @Override
    public MongoCollection<?> resolve(Class<?> entityClass) {
      final String name = ContainerNaming.DEFAULT.name(entityClass);
      // already exists ?
      if (!Flowable.fromPublisher(database.listCollectionNames()).toList().blockingGet().contains(name)) {
        Flowable.fromPublisher(database.createCollection(name)).blockingSubscribe();
      }

      return database.getCollection(name).withDocumentClass(entityClass).withCodecRegistry(registry);
    }
  }

}
