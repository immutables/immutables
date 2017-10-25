package org.immutables.mongo.fixture;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.immutables.check.Checkers.check;

public class SimpleInserterTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final ItemRepository repository = new ItemRepository(context.setup());

  @Test
  public void insertOne() throws Exception {
    Item item1 = ImmutableItem.of("i1");
    repository.insert(item1).getUnchecked();

    check(repository.findAll().fetchAll().getUnchecked()).hasSize(1);
  }

  @Test
  public void insertOne2() throws Exception {
    repository.insert(Collections.singleton(ImmutableItem.of("i1"))).getUnchecked();
    check(repository.findAll().fetchAll().getUnchecked()).hasSize(1);
  }

  @Test
  @Ignore("in mongo v3.4 it doesn't work. insert result is 0")
  public void writeResult() throws Exception {
    check(repository.insert(ImmutableItem.of("i1")).getUnchecked()).is(1);
    check(repository.insert(ImmutableItem.of("i2")).getUnchecked()).is(1);
  }

  @Test
  public void insertDuplicate() throws Exception {
    repository.insert(ImmutableItem.of("i1")).getUnchecked();

    try {
      repository.insert(ImmutableItem.of("i1")).getUnchecked();
    } catch (Exception e) {
      MongoAsserts.assertDuplicateKeyException(e);
    }

    try {
      repository.insert(Collections.singleton(ImmutableItem.of("i1"))).getUnchecked();
    } catch (Exception e) {
      MongoAsserts.assertDuplicateKeyException(e);
    }
  }

  @Test
  @Ignore("This test fails on real mongo: v3.4")
  public void insertMultiple() throws Exception {
    Item item1 = ImmutableItem.of("i1");
    Item item2 = ImmutableItem.of("i2");

    repository.insert(Arrays.asList(item1, item2)).getUnchecked();

    check(repository.findAll().fetchAll().getUnchecked()).hasContentInAnyOrder(item1, item2);
  }

}
