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

import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.reactivex.Flowable;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.immutables.criteria.repository.WriteResult;
import org.immutables.criteria.adapter.Backend;
import org.immutables.criteria.adapter.Operations;
import org.immutables.criteria.repository.UnknownWriteResult;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    this.converter = Mongos.converter();
  }

  private Bson toBson(Query query) {
    return query.filter().map(converter::convert).orElseGet(BsonDocument::new);
  }

  @Override
  public <T> Publisher<T> execute(Operation operation) {
    if (operation instanceof Operations.Select) {
      return query((Operations.Select<T>) operation);
    } else if (operation instanceof Operations.Insert) {
      return (Publisher<T>) insert((Operations.Insert) operation);
    } else if (operation instanceof Operations.Delete) {
      return (Publisher<T>) delete((Operations.Delete) operation);
    } else if (operation instanceof Operations.Watch) {
      return watch((Operations.Watch<T>) operation);
    }

    return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
  }

  private <T> Publisher<T> query(Operations.Select<T> select) {
    @SuppressWarnings("unchecked")
    final MongoCollection<T> collection = (MongoCollection<T>) this.collection;
    final Query query = select.query();
    final FindPublisher<T> find = collection.find(toBson(query));
    if (!query.collations().isEmpty()) {
      // add sorting
      final Function<Collation, Bson> toSortFn = col -> {
        final String path = col.path().toStringPath();
        return col.direction().isAscending() ? Sorts.ascending(path) : Sorts.descending(path);

      };
      final List<Bson> sorts = query.collations().stream()
              .map(toSortFn).collect(Collectors.toList());
      find.sort(Sorts.orderBy(sorts));
    }
    query.limit().ifPresent(limit -> find.limit((int) limit));
    query.offset().ifPresent(offset -> find.skip((int) offset));
    return find;
  }

  private Publisher<WriteResult> delete(Operations.Delete delete) {
    final Bson filter = toBson(delete.query());
    return Flowable.fromPublisher(collection.deleteMany(filter))
            .map(r -> UnknownWriteResult.INSTANCE);
  }

  private Publisher<WriteResult> insert(Operations.Insert insert) {
    final MongoCollection<Object> collection = (MongoCollection<Object>) this.collection;
    final List<Object> values = (List<Object>) insert.values();
    return Flowable.fromPublisher(collection.insertMany(values)).map(r -> UnknownWriteResult.INSTANCE);
  }

  private <T> Publisher<T> watch(Operations.Watch<T> operation) {
    final MongoCollection<T> collection = (MongoCollection<T>) this.collection;
    final Bson filter = new Document("fullDocument", toBson(operation.query()));
    return Flowable.fromPublisher(collection.watch(Collections.singletonList(filter))
            .fullDocument(FullDocument.UPDATE_LOOKUP)
            .withDocumentClass(collection.getDocumentClass()));

  }

}
