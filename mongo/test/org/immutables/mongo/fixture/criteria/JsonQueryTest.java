package org.immutables.mongo.fixture.criteria;

import org.immutables.mongo.fixture.MongoContext;
import org.junit.Rule;
import org.junit.Test;

import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;

/**
 * Test for using repository directly as JSON pass-through.
 */
public class JsonQueryTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final PersonRepository repository = new PersonRepository(context.setup());

  @Test
  public void jsonQuery() throws Exception {
    check(repository.find("{}").fetchAll().getUnchecked()).isEmpty();
    check(repository.find("{ \"_id\": \"1\"}").fetchAll().getUnchecked()).isEmpty();

    final Person john = ImmutablePerson.builder().id("p1").name("John").age(30).build();
    repository.insert(john).getUnchecked();

    check(repository.find("{}").fetchAll().getUnchecked()).hasSize(1);
    check(repository.find("{ \"_id\": \"p1\" }").fetchAll().getUnchecked()).hasSize(1);
    check(repository.find("{ \"_id\": \"__BAD__\" }").fetchAll().getUnchecked()).isEmpty();
    check(repository.find("{ \"name\": \"John\" }").fetchAll().getUnchecked()).hasSize(1);
    check(repository.find("{ \"name\": \"UNKNOWN\" }").fetchAll().getUnchecked()).isEmpty();
    check(repository.find("{ \"age\": 30 }").fetchAll().getUnchecked()).hasSize(1);
    check(repository.find("{ \"age\": 99 }").fetchAll().getUnchecked()).hasSize(0);

    final Person adam = ImmutablePerson.builder().id("p2").name("Adam").age(44).build();
    repository.insert(adam).getUnchecked();
    check(repository.find("{}").fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find("{ \"name\": \"John\" }").fetchAll().getUnchecked()).hasSize(1);
    check(repository.find("{ \"name\": \"Adam\" }").fetchAll().getUnchecked()).hasSize(1);


    check(repository.find("{ \"age\": 30 }").fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find("{ \"age\": 0 }").fetchAll().getUnchecked()).isEmpty();
    check(repository.find("{ \"age\": 44 }").fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
  }

  @Test
  public void badSyntax() throws Exception {

    try {
      repository.find("}  {").fetchAll().getUnchecked();
      fail("Should have failed");
    } catch (Exception ignore) {
      // ok
    }
  }
}
