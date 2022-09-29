/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.immutables.mongo.fixture;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.types.ObjectId;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.mongo.bson4jackson.JacksonCodecs;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.mongo.types.TypeAdapters;
import org.immutables.value.Value;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.UUID;

import static org.immutables.check.Checkers.check;

/**
 * Tests for repository using <a href="https://github.com/FasterXML/jackson">Jackson</a> library.
 *
 * @see JacksonCodecs
 */
public class GsonRepoTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private GsonEntityRepository repository;
  private MongoCollection<BsonDocument> collection;

  @Before
  public void setUp() throws Exception {
    final MongoDatabase database = context.database();
    this.collection = database.getCollection("gsonEntity").withDocumentClass(BsonDocument.class);

    GsonBuilder gson = new GsonBuilder();

    // this one is no longer auto-registered
    gson.registerTypeAdapterFactory(new TypeAdapters());
    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gson.registerTypeAdapterFactory(factory);
    }

    RepositorySetup setup = RepositorySetup.builder()
        .database(database)
        .gson(gson.create())
        .executor(MoreExecutors.newDirectExecutorService())
        .build();

    this.repository = new GsonEntityRepository(setup);
  }

  @Test
  public void withDate() {
    final Date date = new Date();
    final ObjectId id = ObjectId.get();
    final GsonEntity entity = ImmutableGsonEntity.builder()
        .id(id)
        .prop1("prop11")
        .prop2("prop22")
        .date(new Date(date.getTime()))
        .build();

    repository.insert(entity).getUnchecked();

    check(collection.countDocuments()).is(1L);

    // Gson serialises Date using SimpleDateFormat, which loses milliseconds
    final Date expectedDate = Date.from(
        ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.SECONDS)
            .toInstant());
    final GsonEntity expected = ImmutableGsonEntity.builder().from(entity)
        .date(expectedDate)
        .build();

    final GsonEntity actual = repository.findAll().fetchAll().getUnchecked().get(0);
    check(expected).is(actual);

    final BsonDocument doc = collection.find().first();
    check(doc.keySet()).hasContentInAnyOrder("_id", "prop1", "prop_two", "date", "uuid");
    final String expectedDateAsString = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US).format(expectedDate);
    check(doc.get("date").asString().getValue()).is(expectedDateAsString);
    check(doc.get("_id").asObjectId().getValue()).is(id);
  }

  /**
   * persist empty Optional of Date
   */
  @Test
  public void nullDate() {
    final GsonEntity expected = ImmutableGsonEntity.builder()
        .id(ObjectId.get())
        .prop1("prop11")
        .prop2("prop22")
        .build();

    repository.insert(expected).getUnchecked();

    final GsonEntity actual = repository.findAll()
        .fetchAll().getUnchecked().get(0);

    check(expected.date().asSet()).isEmpty();
    check(expected).is(actual);
    final BsonDocument doc = collection.find().first();
    check(doc.keySet()).hasContentInAnyOrder("_id", "prop1", "prop_two", "date", "uuid");
    check(doc.get("date")).is(BsonNull.VALUE);
  }

  @Test
  public void criteria() {
    final Date date = Date.from(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toInstant());

    final ObjectId id = ObjectId.get();
    final UUID uuid = UUID.randomUUID();
    final GsonEntity expected = ImmutableGsonEntity.builder()
        .id(id)
        .prop1("prop11")
        .prop2("prop22")
        .date(date)
        .uuid(uuid)
        .build();

    repository.insert(expected).getUnchecked();

    check(repository.find(repository.criteria().prop1("prop11")).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().prop1("missing")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().prop2("prop22")).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().id(id)).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().date(date)).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().date(new Date(42))).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().uuid(uuid)).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().uuid(UUID.randomUUID())).fetchAll().getUnchecked()).isEmpty();
  }

  @Mongo.Repository
  @Value.Immutable
  @Gson.TypeAdapters
  interface GsonEntity {

    @Mongo.Id
    @JsonProperty("_id")
    ObjectId id();

    String prop1();

    @SerializedName("prop_two")
    String prop2();

    Optional<Date> date();

    @Nullable
    UUID uuid();
  }
}
