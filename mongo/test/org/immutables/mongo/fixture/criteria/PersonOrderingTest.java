package org.immutables.mongo.fixture.criteria;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.immutables.mongo.fixture.MongoContext;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import static org.immutables.check.Checkers.check;

public class PersonOrderingTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final PersonRepository repository = new PersonRepository(context.setup());

  @Test
  public void basicOrdering() throws Exception {
    ImmutablePerson p0 = ImmutablePerson.builder().id("p0").age(30).name("name-p0").build();

    repository.insert(p0).getUnchecked();
    check(repository.findAll().orderByAge().fetchAll().getUnchecked()).hasSize(1);
    check(repository.findAll().orderByAgeDesceding().fetchAll().getUnchecked()).hasSize(1);

    for (int i = 1; i < 4; i++) {
      ImmutablePerson p = p0.withAge(p0.age() + i).withId("p" + i).withName("name-p" + i);
      repository.insert(p).getUnchecked();
    }

    check(repository.findAll().fetchAll().getUnchecked()).hasSize(4);

    // age
    check(Lists.transform(repository.findAll().orderByAge().fetchAll().getUnchecked(), age())).is(Arrays.asList(30, 31, 32, 33));
    check(Lists.transform(repository.findAll().orderByAgeDesceding().fetchAll().getUnchecked(), age())).is(Arrays.asList(33, 32, 31, 30));

    // id
    check(Lists.transform(repository.findAll().orderById().fetchAll().getUnchecked(), id())).is(Arrays.asList("p0", "p1", "p2", "p3"));
    check(Lists.transform(repository.findAll().orderByIdDesceding().fetchAll().getUnchecked(), id())).is(Arrays.asList("p3", "p2", "p1", "p0"));

    // name
    check(Lists.transform(repository.findAll().orderByName().fetchAll().getUnchecked(), name())).is(Arrays.asList("name-p0", "name-p1", "name-p2", "name-p3"));
    check(Lists.transform(repository.findAll().orderByNameDesceding().fetchAll().getUnchecked(), name())).is(Arrays.asList("name-p3", "name-p2", "name-p1", "name-p0"));

  }

  @Test
  public void multiple() throws Exception {
    Person john = ImmutablePerson.builder().id("i1").age(30).name("John").build();
    Person mary = ImmutablePerson.builder().id("i2").age(30).name("Mary").build();
    Person adam = ImmutablePerson.builder().id("i3").age(31).name("Adam").build();

    repository.insert(john).getUnchecked();
    repository.insert(mary).getUnchecked();
    repository.insert(adam).getUnchecked();

    check(repository.findAll().orderByAge().orderByName().fetchAll().getUnchecked()).is(Arrays.asList(john, mary, adam));
    check(repository.findAll().orderByAge().orderByNameDesceding().fetchAll().getUnchecked()).is(Arrays.asList(mary, john, adam));

    check(repository.findAll().orderByName().orderByAge().fetchAll().getUnchecked()).is(Arrays.asList(adam, john, mary));
    check(repository.findAll().orderByNameDesceding().orderByAge().fetchAll().getUnchecked()).is(Arrays.asList(mary, john, adam));
    check(repository.findAll().orderByNameDesceding().orderByAgeDesceding().fetchAll().getUnchecked()).is(Arrays.asList(mary, john, adam));

  }

  private static Function<? super Person, ? extends Integer> age() {
    return new Function<Person, Integer>() {
      @Override
      public Integer apply(Person input) {
        return input.age();
      }
    };
  }

  private static Function<? super Person, String> name() {
    return new Function<Person, String>() {
      @Override
      public String apply(Person input) {
        return input.name();
      }
    };
  }

  private static Function<? super Person, String> id() {
    return new Function<Person, String>() {
      @Override
      public String apply(Person input) {
        return input.id();
      }
    };
  }



  @Test
  public void empty() throws Exception {
    check(repository.findAll().orderByAge().fetchAll().getUnchecked()).isEmpty();
    check(repository.findAll().orderByAgeDesceding().fetchAll().getUnchecked()).isEmpty();
    check(repository.findAll().orderByName().fetchAll().getUnchecked()).isEmpty();
    check(repository.findAll().orderByNameDesceding().fetchAll().getUnchecked()).isEmpty();
    check(repository.findAll().orderByDateOfBirth().fetchAll().getUnchecked()).isEmpty();
    check(repository.findAll().orderByDateOfBirthDesceding().fetchAll().getUnchecked()).isEmpty();
  }
}
