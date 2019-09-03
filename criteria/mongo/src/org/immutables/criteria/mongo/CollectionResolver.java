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
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.criteria.backend.ContainerNaming;
import org.immutables.criteria.backend.ContainerResolver;

import java.util.Objects;

/**
 * {@link MongoCollection} resolver for a particular class
 */
public interface CollectionResolver extends ContainerResolver<MongoCollection<?>> {

  static CollectionResolver defaultResolver(MongoDatabase database, CodecRegistry registry) {
    Objects.requireNonNull(database, "database");
    Objects.requireNonNull(registry, "registry");
    return entityClass -> {
      final String collectionName = ContainerNaming.DEFAULT.name(entityClass);
      return database.getCollection(collectionName)
              .withDocumentClass(entityClass)
              .withCodecRegistry(registry);
    };
  }

}
