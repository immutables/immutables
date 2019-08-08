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
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class MongoSession implements Backend.Session {

  private final ExpressionConverter<Bson> converter;
  private final MongoCollection<?> collection;

  MongoSession(MongoCollection<?> collection, ExpressionConverter<Bson> converter) {
    this.collection = Objects.requireNonNull(collection, "collection");
    this.converter = converter;
  }

  private Bson toBson(Query query) {
    return query.filter().map(converter::convert).orElseGet(BsonDocument::new);
  }

  @Override
  public <X> Publisher<X> execute(Backend.Operation operation) {
    if (operation instanceof StandardOperations.Select) {
      return query((StandardOperations.Select<X>) operation);
    } else if (operation instanceof StandardOperations.Insert) {
      return (Publisher<X>) insert((StandardOperations.Insert) operation);
    } else if (operation instanceof StandardOperations.Delete) {
      return (Publisher<X>) delete((StandardOperations.Delete) operation);
    } else if (operation instanceof StandardOperations.Watch) {
      return watch((StandardOperations.Watch<X>) operation);
    }

    return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
  }

  private <T> Publisher<T> query(StandardOperations.Select<T> select) {
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

  private Publisher<WriteResult> delete(StandardOperations.Delete delete) {
    final Bson filter = toBson(delete.query());
    return Flowable.fromPublisher(collection.deleteMany(filter))
            .map(r -> WriteResult.UNKNOWN);
  }

  private Publisher<WriteResult> insert(StandardOperations.Insert insert) {
    final MongoCollection<Object> collection = (MongoCollection<Object>) this.collection;
    final List<Object> values = (List<Object>) insert.values();
    return Flowable.fromPublisher(collection.insertMany(values)).map(r -> WriteResult.UNKNOWN);
  }

  private <X> Publisher<X> watch(StandardOperations.Watch<X> operation) {
    final MongoCollection<X> collection = (MongoCollection<X>) this.collection;
    final Bson filter = new Document("fullDocument", toBson(operation.query()));
    return Flowable.fromPublisher(collection.watch(Collections.singletonList(filter))
            .fullDocument(FullDocument.UPDATE_LOOKUP)
            .withDocumentClass(collection.getDocumentClass()));

  }

}
