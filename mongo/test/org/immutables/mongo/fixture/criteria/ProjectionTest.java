package org.immutables.mongo.fixture.criteria;

import com.google.common.base.Function;
import org.bson.Document;
import org.immutables.mongo.fixture.MongoContext;
import org.immutables.mongo.repository.Repositories.Projection;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;

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
    Function<Document, String> resultMapper = new Function<Document, String>() {
      @Override
      public String apply(Document document) {
        return document.getString("name");
      }
    };
    Projection<String> projection = Projection.of(resultMapper, "name");
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
    Projection<NameAgeTuple> projection = Projection.of(resultMapper, "name", "age");

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
