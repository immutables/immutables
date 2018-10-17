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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.fakemongo.Fongo;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.types.ObjectId;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.mongo.bson4jackson.BsonGenerator;
import org.immutables.mongo.bson4jackson.BsonParser;
import org.immutables.mongo.bson4jackson.JacksonCodecs;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.value.Value;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static org.immutables.check.Checkers.check;

/**
 * Tests for repository using <a href="https://github.com/FasterXML/jackson">Jackson</a> library.
 * @see JacksonCodecs
 */
@Gson.TypeAdapters // TODO perhaps compile warning (for missing @Gson annotation) can be removed ?
public class JacksonRepoTest {

  private JacksonRepository repository;

  private MongoCollection<BsonDocument> collection;

  @Before
  public void setUp() throws Exception {
    MongoDatabase database = new Fongo("myname").getDatabase("foo");

    this.collection = database.getCollection("jackson").withDocumentClass(BsonDocument.class);

    SimpleModule module = new SimpleModule(); // for our local serializers of Date and ObjectId
    module.addDeserializer(Date.class, new DateDeserializer());
    module.addSerializer(new DateSerializer());
    module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
    module.addSerializer(new ObjectIdSerializer());

    ObjectMapper mapper = new ObjectMapper()
            .registerModule(JacksonCodecs.module(MongoClient.getDefaultCodecRegistry()))
            .registerModule(new GuavaModule())
            .registerModule(module);

    RepositorySetup setup = RepositorySetup.builder()
            .database(database)
            .codecRegistry(JacksonCodecs.registryFromMapper(mapper))
            .executor(MoreExecutors.newDirectExecutorService())
            .build();

    this.repository = new JacksonRepository(setup);
  }

  @Test
  public void withDate() {
    final Date date = new Date();
    final ObjectId id = ObjectId.get();
    final Jackson expected = ImmutableJackson.builder()
            .id(id)
            .prop1("prop1")
            .prop2("22")
            .date(new Date(date.getTime()))
            .build();

    repository.insert(expected).getUnchecked();

    check(collection.count()).is(1L);

    final Jackson actual = repository.findAll().fetchAll().getUnchecked().get(0);
    check(expected).is(actual);

    final BsonDocument doc = collection.find().first();
    check(doc.keySet()).hasContentInAnyOrder("_id", "prop1", "prop2", "date");
    check(doc.get("date").asDateTime().getValue()).is(date.getTime());
    check(doc.get("_id").asObjectId().getValue()).is(id);
  }

  /**
   * persist empty Optional of Date
   */
  @Test
  public void nullDate() {
    final Jackson expected = ImmutableJackson.builder()
            .id(ObjectId.get())
            .prop1("prop11")
            .prop2("prop22")
            .build();

    repository.insert(expected).getUnchecked();

    final Jackson actual = repository.findAll()
            .fetchAll().getUnchecked().get(0);

    check(expected.date().asSet()).isEmpty();
    check(expected).is(actual);
    final BsonDocument doc = collection.find().first();
    check(doc.keySet()).hasContentInAnyOrder("_id", "prop1", "prop2", "date");
    check(doc.get("date")).is(BsonNull.VALUE);
  }

  @Test
  public void criteria() {
    final Date date = new Date();

    final ObjectId id = ObjectId.get();
    final Jackson expected = ImmutableJackson.builder()
            .id(id)
            .prop1("prop11")
            .prop2("prop22")
            .date(date)
            .build();

    repository.insert(expected).getUnchecked();

    check(repository.find(repository.criteria().prop1("prop11")).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().prop1("missing")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().prop2("prop22")).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().id(id)).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().date(date)).fetchAll().getUnchecked()).hasContentInAnyOrder(expected);
    check(repository.find(repository.criteria().date(new Date(42))).fetchAll().getUnchecked()).isEmpty();
  }

  @Mongo.Repository
  @Value.Immutable
  @JsonDeserialize(as = ImmutableJackson.class)
  @JsonSerialize(as = ImmutableJackson.class)
  interface Jackson {

    @Mongo.Id
    @JsonProperty("_id")
    ObjectId id();

    String prop1();

    // TODO Allow querying on changed properties via @JsonPropery("changedProp2")
    String prop2();

    Optional<Date> date();
  }

  /**
   * Custom deserializer for {@link BsonType#OBJECT_ID}
   */
  private static class ObjectIdDeserializer extends StdScalarDeserializer<ObjectId> {

    private ObjectIdDeserializer() {
      super(ObjectId.class);
    }

    @Override
    public ObjectId deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      final BsonReader reader = ((BsonParser) parser).unwrap();
      return reader.readObjectId();
    }
  }

  /**
   * Custom serializer for {@link BsonType#OBJECT_ID}
   */
  private static class ObjectIdSerializer extends StdScalarSerializer<ObjectId> {

    private ObjectIdSerializer() {
      super(ObjectId.class);
    }

    @Override
    public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      final BsonWriter writer = ((BsonGenerator) gen).unwrap();
      writer.writeObjectId(value);
    }
  }

  /**
   * Custom deserializer for {@link BsonType#DATE_TIME}
   */
  private static class DateDeserializer extends StdScalarDeserializer<Date> {

    private DateDeserializer() {
      super(Date.class);
    }

    @Override
    public Date deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      final BsonReader reader = ((BsonParser) parser).unwrap();
      return new Date(reader.readDateTime());
    }
  }

  /**
   * Custom serializer to test {@link BsonType#DATE_TIME} in BSON
   */
  private static class DateSerializer extends StdScalarSerializer<Date> {

    private DateSerializer() {
      super(Date.class);
    }

    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      final BsonWriter writer = ((BsonGenerator) gen).unwrap();
      writer.writeDateTime(value.getTime());
    }
  }


}
