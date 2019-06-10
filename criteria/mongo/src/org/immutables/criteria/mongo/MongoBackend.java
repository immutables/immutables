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

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.Repository;
import org.immutables.criteria.adapter.Backend;
import org.immutables.criteria.adapter.Operations;
import org.immutables.criteria.adapter.Reactive;
import org.immutables.criteria.expression.ExpressionConverter;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.Objects;

/**
 * Allows to query and modify mongo documents using criteria API.
 *
 * <p>Based on <a href="https://mongodb.github.io/mongo-java-driver-reactivestreams/">Mongo reactive streams driver</a>
 */
class MongoBackend implements Backend {

  private final MongoCollection<?> collection;
  private final ExpressionConverter<Bson> converter;

  MongoBackend(MongoCollection<?> collection) {
    this.collection = Objects.requireNonNull(collection, "collection");
    this.converter = Mongos.converter(collection.getCodecRegistry());
  }

  private Bson toBson(DocumentCriteria<?> criteria) {
    return converter.convert(Criterias.toExpression(criteria));
  }

  @Override
  public <T> Publisher<T> execute(Operation<T> operation) {
    if (operation instanceof Operations.Query) {
      return query((Operations.Query<T>) operation);
    } else if (operation instanceof Operations.Insert) {
      return (Publisher<T>) insert((Operations.Insert) operation);
    } else if (operation instanceof Operations.Delete) {
      return (Publisher<T>) delete((Operations.Delete) operation);
    } else if (operation instanceof Operations.Watch) {
      return watch((Operations.Watch<T>) operation);
    }

    return Reactive.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
  }

  private <T> Publisher<T> query(Operations.Query<T> query) {
    @SuppressWarnings("unchecked")
    final MongoCollection<T> collection = (MongoCollection<T>) this.collection;
    return collection.find(toBson(query.criteria()));
  }

  private Publisher<Repository.Success> delete(Operations.Delete delete) {
    final Bson filter = toBson(delete.criteria());
    return Reactive.map(collection.deleteMany(filter), r -> Repository.Success.SUCCESS);
  }

  private Publisher<Repository.Success> insert(Operations.Insert insert) {
    final MongoCollection<Object> collection = (MongoCollection<Object>) this.collection;
    return Reactive.map(collection.insertMany(insert.entities()), r -> Repository.Success.SUCCESS);
  }

  private <T> Publisher<T> watch(Operations.Watch<T> operation) {
    final MongoCollection<T> collection = (MongoCollection<T>) this.collection;
    final Bson filter = new Document("fullDocument", toBson(operation.criteria()));
    return collection.watch(Collections.singletonList(filter))
            .fullDocument(FullDocument.UPDATE_LOOKUP)
            .withDocumentClass(collection.getDocumentClass());

  }

}
