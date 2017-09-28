package org.immutables.mongo.fixture.criteria;

import org.immutables.mongo.fixture.MongoContext;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static org.immutables.check.Checkers.check;

/**
 * Test of excludes
 */
public class CriteriaExcludeTest {
  @Rule
  public final MongoContext context = MongoContext.create();

  private final PersonRepository repository = new PersonRepository(context.setup());

  @Test
  public void excludeWhenEmptyResult() throws Exception {
    repository.findAll().excludeAliases().fetchAll().getUnchecked();
    repository.findAll().excludeAliases().excludeDateOfBirth().fetchAll().getUnchecked();
    repository.findAll().excludeAliases().excludeDateOfBirth().fetchFirst().getUnchecked();
  }

  /**
   * Interesting this also fails on Real mongo database:
   *
   *  On read
   * {@code
   * Cannot build Person, some of required attributes are not set [name, age]:
   * }
   */
  @Test
  @Ignore
  public void exclude1() throws Exception {
    Person john = ImmutablePerson.builder().id("p1").name("john").dateOfBirth(new Date())
            .aliases(Arrays.asList("a1", "a2"))
            .age(30)
            .build();

    repository.insert(john).getUnchecked();

    check(repository.findAll().excludeDateOfBirth().fetchFirst().getUnchecked().get().dateOfBirth()).isAbsent();

    // still has aliases
    check(repository.findAll().excludeDateOfBirth().fetchFirst().getUnchecked().get().aliases())
            .hasAll("a1", "a2");

    check(repository.findAll().excludeAliases().fetchFirst().getUnchecked().get().dateOfBirth()).isPresent();
    check(repository.findAll().excludeAliases().fetchFirst().getUnchecked().get().aliases()).isEmpty();
    check(repository.findAll().excludeAliases().excludeDateOfBirth().fetchFirst().getUnchecked().get().dateOfBirth()).isAbsent();
    check(repository.findAll().excludeAliases().excludeDateOfBirth().fetchFirst().getUnchecked().get().aliases()).isEmpty();
  }
}
