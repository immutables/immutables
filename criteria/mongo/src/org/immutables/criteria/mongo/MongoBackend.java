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

import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.conversions.Bson;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.internal.Backend;
import org.immutables.criteria.internal.Query;
import org.reactivestreams.Publisher;

import java.util.Objects;

/**
 * Allows to query and modify mongo documents using criteria API.
 *
 * <p>Based on <a href="https://mongodb.github.io/mongo-java-driver-reactivestreams/">Mongo reactive streams driver</a>
 */
class MongoBackend implements Backend {

  private final MongoCollection<?> collection;

  MongoBackend(MongoCollection<?> collection) {
    this.collection = Objects.requireNonNull(collection, "collection");
  }

  @Override
  public <T> Publisher<T> execute(Operation<T> operation) {
    @SuppressWarnings("unchecked")
    final MongoCollection<T> collection = (MongoCollection<T>) this.collection;
    final Bson filter = Mongos.converter(collection.getCodecRegistry()).convert(Criterias.toExpression(((Query) operation).criteria()));
    return collection.find(filter);
  }

}
