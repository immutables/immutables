package org.immutables.mongo.fixture;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import static org.immutables.check.Checkers.check;

public class SimpleDeleterTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final ItemRepository repository = new ItemRepository(context.setup());

  @Test
  public void basicDelete() throws Exception {
    final ImmutableItem item1 = ImmutableItem.of("id1");

    check(repository.find(repository.criteria()).deleteAll().getUnchecked()).is(0);
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isAbsent();

    repository.insert(item1).getUnchecked();
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isOf(item1);
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isAbsent();
    check(repository.find(repository.criteria()).deleteAll().getUnchecked()).is(0);

    repository.insert(item1).getUnchecked();
    check(repository.find(repository.criteria()).deleteAll().getUnchecked()).is(1);
    check(repository.find(repository.criteria()).deleteAll().getUnchecked()).is(0);
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isAbsent();

    final ImmutableItem item2 = ImmutableItem.of("id2");

    repository.insert(item1).getUnchecked();
    repository.insert(item2).getUnchecked();

    check(repository.find(repository.criteria()).deleteAll().getUnchecked()).is(2);
    check(repository.find(repository.criteria()).deleteAll().getUnchecked()).is(0);
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isAbsent();

    repository.insert(item1).getUnchecked();
    repository.insert(item2).getUnchecked();
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isPresent();
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isPresent();
    check(repository.find(repository.criteria()).deleteFirst().getUnchecked()).isAbsent(); // third delete empty
  }

  @Test
  public void deleteByCriteria() throws Exception {
    check(repository.find(repository.criteria().id("_MISSING_")).deleteAll().getUnchecked()).is(0);
    check(repository.find(repository.criteria().id("_MISSING_")).deleteFirst().getUnchecked()).isAbsent();

    final ImmutableItem item1 = ImmutableItem.of("id1");
    repository.insert(item1).getUnchecked();

    check(repository.find(repository.criteria().id("_MISSING_")).deleteAll().getUnchecked()).is(0);
    check(repository.find(repository.criteria().id("_MISSING_")).deleteFirst().getUnchecked()).isAbsent();

    check(repository.find(repository.criteria().id(item1.id())).deleteAll().getUnchecked()).is(1);
    check(repository.find(repository.criteria().id(item1.id())).deleteAll().getUnchecked()).is(0);
    check(repository.find(repository.criteria().id("_MISSING_")).deleteAll().getUnchecked()).is(0);

    repository.insert(item1).getUnchecked();
    check(repository.find(repository.criteria().id(item1.id())).deleteFirst().getUnchecked()).isOf(item1);
    check(repository.find(repository.criteria().id(item1.id())).deleteFirst().getUnchecked()).isAbsent();
    check(repository.find(repository.criteria().id(item1.id())).deleteAll().getUnchecked()).is(0);


    final ImmutableItem item2 = ImmutableItem.of("id2");
    repository.insert(item1).getUnchecked();
    repository.insert(item2).getUnchecked();

    check(repository.find(repository.criteria().id(item1.id())).deleteFirst().getUnchecked()).isOf(item1);
    check(repository.find(repository.criteria().id(item1.id())).deleteFirst().getUnchecked()).isAbsent();

    check(repository.find(repository.criteria().id(item2.id())).deleteFirst().getUnchecked()).isOf(item2);
    check(repository.find(repository.criteria().id(item2.id())).deleteFirst().getUnchecked()).isAbsent();
    check(repository.find(repository.criteria().id(item1.id())).deleteAll().getUnchecked()).is(0);

    repository.insert(item1).getUnchecked();
    repository.insert(item2).getUnchecked();
    check(repository.find(repository.criteria().idIn(Arrays.asList("id1", "id2"))).deleteAll().getUnchecked()).is(2);
    check(repository.find(repository.criteria().idIn(Arrays.asList("id1", "id2"))).deleteAll().getUnchecked()).is(0);
  }
}
