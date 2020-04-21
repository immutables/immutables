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

import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.BackendException;
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.ExpressionNaming;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.UniqueCachedNaming;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Visitors;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
class MongoSession implements Backend.Session {

  private static final Logger logger = Logger.getLogger(MongoSession.class.getName());

  private final ExpressionConverter<Bson> converter;
  private final MongoCollection<?> collection;
  private final PathNaming pathNaming;
  private final KeyExtractor keyExtractor;

  MongoSession(MongoCollection<?> collection, KeyExtractor keyExtractor, PathNaming pathNaming) {
    this.collection = Objects.requireNonNull(collection, "collection");
    this.converter = Mongos.converter(pathNaming);
    this.keyExtractor = Objects.requireNonNull(keyExtractor, "keyExtractor");
    this.pathNaming = pathNaming;
  }

  private Bson toBsonFilter(Query query) {
    Bson bson = query.filter().map(converter::convert).orElseGet(BsonDocument::new);
    if (logger.isLoggable(Level.FINE)) {
      BsonDocument filter = bson.toBsonDocument(BsonDocument.class, collection.getCodecRegistry());
      logger.log(Level.FINE, "Using filter [{0}] to query {1}", new Object[] {filter, collection.getNamespace()});
    }
    return bson;
  }

  @Override
  public Class<?> entityType() {
    return collection.getDocumentClass();
  }

  @Override
  public Backend.Result execute(Backend.Operation operation) {
    return DefaultResult.of(executeInternal(operation));
  }

  private Publisher<?> executeInternal(Backend.Operation operation) {
    Publisher<?> publisher;
    if (operation instanceof StandardOperations.Select) {
      publisher =  query((StandardOperations.Select) operation);
    } else if (operation instanceof StandardOperations.Insert) {
      publisher = insert((StandardOperations.Insert) operation);
    } else if (operation instanceof StandardOperations.Delete) {
      publisher = delete((StandardOperations.Delete) operation);
    } else if (operation instanceof StandardOperations.Watch) {
      publisher = watch((StandardOperations.Watch) operation);
    } else if (operation instanceof StandardOperations.UpdateByQuery) {
      publisher = updateByQuery((StandardOperations.UpdateByQuery) operation);
    } else if (operation instanceof StandardOperations.Update) {
      publisher = update((StandardOperations.Update) operation);
    } else {
      return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
    }

    return Flowable.fromPublisher(publisher).compose(wrapMongoException());
  }

  private Publisher<?> query(StandardOperations.Select select) {
    final Query query = select.query();

    final boolean hasProjections = query.hasProjections();

    boolean useAggregationPipeline = query.hasAggregations() || query.distinct();
    ExpressionNaming expressionNaming = useAggregationPipeline ? ExpressionNaming.from(UniqueCachedNaming.of(query.projections())) : expression -> pathNaming.name((Path) expression);

    MongoCollection<?> collection = this.collection;
    if (hasProjections) {
      // add special TupleCodecProvider for projections
      CodecRegistry newRegistry = CodecRegistries.fromRegistries(this.collection.getCodecRegistry(),
              CodecRegistries.fromProviders(new TupleCodecProvider(query, expressionNaming)));

      collection = this.collection.withDocumentClass(ProjectedTuple.class)
              .withCodecRegistry(newRegistry);
    }

    if (useAggregationPipeline) {
      // aggregations
      AggregationQuery agg = new AggregationQuery(query, pathNaming);
      if (query.count()) {
        // count will return single document like {count:1}
        // also for empty result-set mongo does not return single(0) but empty publisher
        return Flowable.fromPublisher(collection.aggregate(agg.toPipeline(), BsonDocument.class))
                .map(d -> d.get("count").asNumber().longValue())
                .defaultIfEmpty(0L); // return Single.just(0) for empty publisher
      }
      return collection.aggregate(agg.toPipeline(), ProjectedTuple.class);
    }

    Bson filter = toBsonFilter(query);

    if (query.count()) {
      // simple form of count all (without distinct, aggregations etc.) : count(*)
      return Flowable.fromPublisher(collection.countDocuments(filter));
    }

    final FindPublisher<?> find = collection.find(filter);
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


    if (hasProjections) {
      List<String> fields = query.projections()
              .stream().map(p -> pathNaming.name((Path) p))
              .collect(Collectors.toList());

      find.projection(Projections.include(fields));
      return find;
    }

    // post-process result with projections
    return find;
  }

  private static <T> FlowableTransformer<T, T> wrapMongoException() {
    Function<Throwable, Throwable> mapFn = e -> e instanceof MongoException ? new BackendException("failed to update", e) : e;
    return flowable -> flowable.onErrorResumeNext((Throwable e) -> Flowable.error(mapFn.apply(e)));
  }

  /**
   * Convert generic object to a BsonValue
   */
  private BsonValue toBsonValue(Object value) {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    writer.writeStartDocument();
    writer.writeName("value");
    Codec<Object> codec = (Codec<Object>) collection.getCodecRegistry().get(value.getClass());
    codec.encode(writer, value, EncoderContext.builder().build());
    writer.writeEndDocument();
    return writer.getDocument().get("value");
  }

  /**
   * Uses <a href="https://docs.mongodb.com/manual/reference/method/db.collection.replaceOne/">replaceOne</a> operation
   * with <a href="https://docs.mongodb.com/manual/reference/method/db.collection.bulkWrite/">bulkWrite</a>. Right now has to convert
   * object to BsonDocument to extract {@code _id} attribute.
   */
  private <T> Publisher<WriteResult> update(StandardOperations.Update operation) {
    ReplaceOptions options = new ReplaceOptions();
    if (operation.upsert()) {
      options.upsert(operation.upsert());
    }

    List<ReplaceOneModel<Object>> docs =  operation.values().stream()
            .map(value -> new ReplaceOneModel<>(new BsonDocument("_id", toBsonValue(keyExtractor.extract(value))), value, options))
            .collect(Collectors.toList());

    Publisher<BulkWriteResult> publisher = ((MongoCollection<Object>) collection).bulkWrite(docs);
    return Flowable.fromPublisher(publisher).map(x -> WriteResult.unknown());
  }

  private Publisher<WriteResult> updateByQuery(StandardOperations.UpdateByQuery operation) {

    Optional<Object> replace = operation.replace();
    if (replace.isPresent()) {
      return Flowable.error(new UnsupportedOperationException("Replacing whole objects not yet supported by " + MongoBackend.class.getSimpleName()));
    }

    Bson filter = toBsonFilter(operation.query());
    Document set = new Document();
    operation.values().forEach((path, value) -> {
      if (path.returnType() != Optional.class && Optional.empty().equals(value)) {
        value = null; // unwrap from Optional
      }
      set.put(Visitors.toPath(path).toStringPath(), value);
    });

    return Flowable.fromPublisher(collection.updateMany(filter, new Document("$set", set)))
            .map(x -> WriteResult.unknown());

  }

  private Publisher<WriteResult> delete(StandardOperations.Delete delete) {
    final Bson filter = toBsonFilter(delete.query());
    return Flowable.fromPublisher(collection.deleteMany(filter))
            .map(r -> WriteResult.unknown());
  }

  private Publisher<WriteResult> insert(StandardOperations.Insert insert) {
    final MongoCollection<Object> collection = (MongoCollection<Object>) this.collection;
    final List<Object> values = insert.values();
    return Flowable.fromPublisher(collection.insertMany(values)).map(r -> WriteResult.unknown());
  }

  private <X> Publisher<X> watch(StandardOperations.Watch operation) {
    final MongoCollection<X> collection = (MongoCollection<X>) this.collection;
    final Bson filter = new Document("fullDocument", toBsonFilter(operation.query()));
    return Flowable.fromPublisher(collection.watch(Collections.singletonList(filter))
            .fullDocument(FullDocument.UPDATE_LOOKUP)
            .withDocumentClass(collection.getDocumentClass()));

  }

}
