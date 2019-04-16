package org.immutables.mongo.fixture.criteria;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.mongo.bson4gson.GsonCodecs;
import org.immutables.mongo.fixture.MongoContext;
import org.immutables.mongo.repository.Repositories.Projection;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;

import static org.immutables.check.Checkers.check;

/**
 * Test of excludes
 */
public class ProjectionTest {
  @Rule
  public final MongoContext context = MongoContext.create();

  private final PersonRepository repository = new PersonRepository(context.setup());

  @Test
  public void projectToDocument() throws Exception {
    Projection<Document> projection = Projection.of(Document.class, "name");
    check(repository.findAll().fetchFirst(projection).getUnchecked()).isAbsent();
    check(repository.findAll().fetchAll(projection).getUnchecked()).isEmpty();

    Person adam = ImmutablePerson.builder().id("p1").name("adam").dateOfBirth(new Date())
            .aliases(Arrays.asList("a1"))
            .age(35)
            .build();
    Person john = ImmutablePerson.builder().id("p2").name("john").dateOfBirth(new Date())
            .aliases(Arrays.asList("j1", "j2"))
            .age(30)
            .build();

    repository.insert(adam).getUnchecked();
    repository.insert(john).getUnchecked();

    Document adamDoc = new Document("name", "adam");
    Document johnDoc = new Document("name", "john");
    check(repository.findAll().fetchFirst(projection).getUnchecked()).isOf(adamDoc);
    check(repository.findAll().fetchAll(projection).getUnchecked()).hasContentInAnyOrder(adamDoc, johnDoc);
    check(repository.findAll().fetchWithLimit(1, projection).getUnchecked()).hasContentInAnyOrder(adamDoc);
    check(repository.findAll().skip(1).fetchWithLimit(1, projection).getUnchecked()).hasContentInAnyOrder(johnDoc);
  }

  @Test
  public void projectToCustomResultType() throws Exception {
    Person adam = ImmutablePerson.builder().id("p1").name("adam").dateOfBirth(new Date())
            .aliases(Arrays.asList("a1"))
            .age(35)
            .build();
    Person john = ImmutablePerson.builder().id("p2").name("john").dateOfBirth(new Date())
            .aliases(Arrays.asList("j1", "j2"))
            .age(30)
            .build();

    repository.insert(adam).getUnchecked();
    repository.insert(john).getUnchecked();

    Function<Document, NameAgeTuple> resultMapper = new Function<Document, NameAgeTuple>() {
      @Override
      public NameAgeTuple apply(Document document) {
        return ImmutableNameAgeTuple.of(document.getString("name"), document.getLong("age"));
      }
    };
    Projection<NameAgeTuple> projection = new Projection<NameAgeTuple>() {
      @Override
      public CodecRegistry codecRegistry() {
        Gson gson = new GsonBuilder().registerTypeAdapter(NameAgeTuple.class, new JsonDeserializer<NameAgeTuple>() {
          @Override
          public NameAgeTuple deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String name = obj.get("name").getAsString();
            long age = obj.get("age").getAsLong();
            return ImmutableNameAgeTuple.of(name, age);
          }
        }).create();
        return GsonCodecs.codecRegistryFromGson(gson);
      }

      @Override
      public Bson fields() {
        return Projections.fields(Projections.include("name", "age"));
      }

      @Override
      public Class<NameAgeTuple> resultType() {
        return NameAgeTuple.class;
      }
    };

    NameAgeTuple adamTuple = ImmutableNameAgeTuple.of("adam", 35);
    NameAgeTuple johnTuple = ImmutableNameAgeTuple.of("john", 30);
    check(repository.findAll().orderByName().fetchFirst(projection).getUnchecked().get()).is(adamTuple);
    check(repository.findAll().fetchAll(projection).getUnchecked()).hasContentInAnyOrder(adamTuple, johnTuple);
    check(repository.findAll().orderByName().skip(1).fetchAll(projection).getUnchecked()).hasContentInAnyOrder(johnTuple);
  }

  @Value.Immutable
  interface NameAgeTuple {
    @Value.Parameter(order = 0)
    String name();
    @Value.Parameter(order = 1)
    long age();
  }

}
