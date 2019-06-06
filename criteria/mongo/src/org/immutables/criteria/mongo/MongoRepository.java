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
import com.mongodb.reactivestreams.client.Success;
import org.bson.conversions.Bson;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.Repository;
import org.reactivestreams.Publisher;

import java.util.Objects;

/**
 * Allows to query and modify mongo documents using criteria API.
 *
 * <p>Based on <a href="https://mongodb.github.io/mongo-java-driver-reactivestreams/">Mongo reactive streams driver</a>
 */
class MongoRepository<T> implements Repository<T> {

  private final MongoCollection<T> collection;

  MongoRepository(MongoCollection<T> collection) {
    this.collection = Objects.requireNonNull(collection, "collection");
  }

  @Override
  public Finder<T> find(DocumentCriteria<T> criteria) {
    return new Finder<>(collection, criteria);
  }

  // which one is _id ?
  public Publisher<Success> insert(T entity) {
    return collection.insertOne(entity);
  }

  public static class Finder<T> implements Repository.Finder<T> {

    private final DocumentCriteria<T> criteria;
    private final MongoCollection<T> collection;

    private Finder(MongoCollection<T> collection, DocumentCriteria<T> criteria) {
      this.criteria = Objects.requireNonNull(criteria, "criteria");
      this.collection = Objects.requireNonNull(collection, "collection");
    }

    @Override
    public Publisher<T> fetch() {
      final Bson filter = Mongos.toBson(collection.getCodecRegistry(), Criterias.toExpression(criteria));
      return collection.find(filter);
    }
  }

}
