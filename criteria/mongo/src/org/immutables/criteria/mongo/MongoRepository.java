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
