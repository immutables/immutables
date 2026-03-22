package org.immutables.criteria.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.Flowable;
import org.bson.BsonDocument;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.backend.*;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.mongo.bson4jackson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.OptionalLong;

import static org.immutables.check.Checkers.check;

/**
 * Tests tighter integration between MongoDB and Jackson.
 *
 * Verifies that criteria operations correctly resolve BSON field names when using
 * Jackson introspection, especially for {@code @JsonProperty} renames and bean getters.
 */
@ExtendWith(MongoExtension.class)
class JacksonMongoTest {

  // Reusing existing classes from other tests.
  private final Bean1Criteria bean1 = Bean1Criteria.bean1;

  private final MongoSetup setup;
  private final MongoSession session;
  private final MongoCollection<BsonDocument> collection;

  JacksonMongoTest(MongoDatabase database) {
    BackendResource resource = new BackendResource(database);
    // Force jackson path naming.
    PathNaming naming = new JacksonPathNaming(resource.mapper());
    this.setup = ImmutableMongoSetup.copyOf(resource.setup()).withPathNaming(naming);
    Class<JacksonPathNamingTest.Bean1> entityType = JacksonPathNamingTest.Bean1.class;
    this.session = new MongoSession(setup.collectionResolver().resolve(entityType),
        setup.keyExtractorFactory().create(entityType), setup.pathNaming());
    this.collection = setup.collectionResolver().resolve(entityType)
        .withDocumentClass(BsonDocument.class);
  }

  private WriteResult execute(Backend.Operation operation) {
    return (WriteResult) Flowable.fromPublisher(session.execute(operation).publisher())
        .blockingFirst();
  }

  private BsonDocument findById(String id) {
    return Flowable.fromPublisher(collection.find(Filters.eq("_id", id))).blockingFirst();
  }

  @Test
  void insert() {
    JacksonPathNamingTest.Bean1 bean = ImmutableBean1.builder()
        .id("id1")
        .prop1("p1")
        .prop2("p2")
        .prop3("p3")
        .isProp4(true)
        .build();

    StandardOperations.Insert insert = StandardOperations.Insert.ofValues(Collections.singletonList(bean));
    check(execute(insert).insertedCount()).is(OptionalLong.of(1));

    final BsonDocument expected = BsonDocument.parse(String.format("{_id:'%s', prop1: 'p1',  prop2: 'p2', " +
        "prop3_changed: 'p3', prop4: true}", bean.getId()));
    check(findById(bean.getId())).is(expected);
  }

  /**
   * Verifies that field names in {@code UpdateByQuery} are correctly resolved when using
   * Jackson path naming (e.g. bean getters and {@code @JsonProperty} renames).
   */
  @Test
  void updateByQuery() {
    JacksonPathNamingTest.Bean1 bean = ImmutableBean1.builder()
            .id("id1")
            .prop1("p1")
            .prop2("p2")
            .prop3("p3")
            .isProp4(true)
            .build();

    StandardOperations.Insert insert = StandardOperations.Insert.ofValues(Collections.singletonList(bean));
    check(execute(insert).insertedCount()).is(OptionalLong.of(1));

    // Execute an update without repository.
    StandardOperations.UpdateByQuery update = ImmutableUpdateByQuery.builder()
            .query(Criterias.toQuery(bean1.id.is(bean.getId())))
            .putValues(Matchers.toExpression(bean1.prop1), "p1-updated")
            .putValues(Matchers.toExpression(bean1.prop2), "p2-updated")
            .putValues(Matchers.toExpression(bean1.prop3), "p3-updated")
            .putValues(Matchers.toExpression(bean1.isProp4), false)
            .build();

    check(execute(update).updatedCount()).is(OptionalLong.of(1));

    // Get that all fields are correctly named after update.
    BsonDocument actual = findById(bean.getId());

    check(!actual.isEmpty());
    final BsonDocument expected =  BsonDocument.parse(String.format("{_id:'%s', prop1: 'p1-updated',  prop2: 'p2-updated', " +
        "prop3_changed: 'p3-updated', prop4: false}", bean.getId()));
    check(actual).is(expected);
  }
}
