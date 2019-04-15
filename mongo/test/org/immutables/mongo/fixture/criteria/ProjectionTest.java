package org.immutables.mongo.fixture.criteria;

import com.google.common.base.Function;
import com.google.gson.*;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
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
  public void projectWhenEmptyResult() throws Exception {
    Projection<Document> projection = new Projection<Document>() {
      @Override
      protected Gson gson() {
        return null;
      }

      @Override
      protected Bson fields() {
        return Projections.include("name");
      }

      @Override
      protected Class<Document> resultType() {
        return Document.class;
      }
    };
    check(repository.findAll().fetchFirst(projection).getUnchecked()).isAbsent();
    check(repository.findAll().fetchAll(projection).getUnchecked()).isEmpty();
  }

  @Test
  public void project() throws Exception {
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
      protected Gson gson() {
        return new GsonBuilder().registerTypeAdapter(NameAgeTuple.class, new JsonDeserializer<NameAgeTuple>() {
          @Override
          public NameAgeTuple deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String name = obj.get("name").getAsString();
            long age = obj.get("age").getAsLong();
            return ImmutableNameAgeTuple.of(name, age);
          }
        }).create();
      }

      @Override
      protected Bson fields() {
        return Projections.fields(Projections.include("name", "age"));
      }

      @Override
      protected Class<NameAgeTuple> resultType() {
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
