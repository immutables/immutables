package org.immutables.criteria.mongo;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.Success;
import org.bson.conversions.Bson;
import org.immutables.criteria.constraints.Expressional;
import org.reactivestreams.Publisher;

import java.util.Objects;

/**
 * Allows to query mongo documents using criteria API.
 *
 * <p>Based on <a href="https://mongodb.github.io/mongo-java-driver-reactivestreams/">Mongo reactive streams driver</a>
 */
class MongoRepository<T> {

  private final MongoCollection<T> collection;

  MongoRepository(MongoCollection<T> collection) {
    this.collection = Objects.requireNonNull(collection, "collection");
  }


  public Publisher<T> query(Expressional<T> expressional) {
    final Bson filter = Mongos.toBson(collection.getCodecRegistry(), expressional.expression());
    return collection.find(filter);
  }

  public Publisher<Success> insert(T entity) {
    return collection.insertOne(entity);
  }

}
