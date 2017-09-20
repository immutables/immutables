/*
   Copyright 2017 Immutables Authors and Contributors

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

import org.immutables.mongo.types.Binary;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;

/**
 * Basic CRUD operations on the top of repository
 */
public class BasicMongoOperationsTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final ItemRepository repository = new ItemRepository(context.setup());

  @Test
  public void empty() throws Exception {
    check(repository.findAll().fetchAll().getUnchecked()).isEmpty();
    check(repository.findAll().fetchFirst().getUnchecked()).isAbsent();
    check(repository.findById("").fetchAll().getUnchecked()).isEmpty();
    check(repository.findById("MISSING").fetchAll().getUnchecked()).isEmpty();
  }

  @Test
  public void readWrite_single() throws Exception {
    Item item = ImmutableItem.builder()
        .id("1")
        .binary(Binary.create(new byte[] {1, 2, 3}))
        .build();

    check(repository.upsert(item).getUnchecked()).is(1);

    List<Item> items = repository.find(repository.criteria().id("1")).fetchAll().getUnchecked();

    check(items).hasSize(1);
    check(items.get(0).id()).is("1");
    check(repository.findById("1").fetchAll().getUnchecked()).hasSize(1);
  }

  @Test
  public void insert() throws Exception {
    repository.insert(item()).getUnchecked();
    check(repository.findAll().fetchAll().getUnchecked()).hasSize(1);
    repository.insert(item().withId("another_id")).getUnchecked();
    check(repository.findAll().fetchAll().getUnchecked()).hasSize(2);

    try {
      repository.insert(item()).getUnchecked();
      fail("Didn't fail when duplicate key inserted");
    } catch (Exception ignore) {
    }

    check(repository.findAll().fetchAll().getUnchecked()).hasSize(2);
  }

  @Test
  public void upsert() throws Exception {
    repository.upsert(item()).getUnchecked();
    check(findItem()).is(item());

    repository.upsert(item().withList("foo", "bar")).getUnchecked();
    check(findItem().list()).hasAll("foo", "bar");

    // add item with different ID. This should be insert (not update)
    repository.upsert(item().withId("another_id").withList("aaa")).getUnchecked();
    check(repository.findAll().fetchAll().getUnchecked()).hasSize(2);
  }

  @Test
  public void update() throws Exception {
    ImmutableItem item = item();
    repository.insert(item).getUnchecked();

    check(repository.update(repository.criteria().id(item.id()))
            .addList("foo").updateAll().getUnchecked())
        .is(1);

    check(repository.findAll().fetchAll().getUnchecked()).hasSize(1);
    check(repository.findAll().fetchAll().getUnchecked().get(0).list()).hasAll("foo");
  }

  @Test
  public void delete() throws Exception {
    Item item = item();

    repository.insert(item).getUnchecked();

    // expect single entry to be deleted
    check(repository.findAll().deleteAll().getUnchecked()).is(1);
    // second time no entries remaining
    check(repository.findAll().deleteAll().getUnchecked()).is(0);
  }

  @Test
  public void index() throws Exception {
    repository.index().withId().ensure().getUnchecked();
  }

  @Test
  public void jsonQuery() throws Exception {
    check(repository.find("{}").fetchAll().getUnchecked()).isEmpty();
    check(repository.find("{ \"_id\": \"1\"}").fetchAll().getUnchecked()).isEmpty();
    Item item = item().withList("foo");
    repository.insert(item).getUnchecked();

    check(repository.find("{ \"_id\": \"1\" }").fetchAll().getUnchecked()).hasSize(1);
    check(repository.find("{ \"_id\": \"99\" }").fetchAll().getUnchecked()).isEmpty();
    check(repository.find("{ \"list\": \"foo\" }").fetchAll().getUnchecked()).hasSize(1);
    check(repository.find("{ \"list\": \"bar\" }").fetchAll().getUnchecked()).isEmpty();
  }

  private Item findItem() {
    return repository.findById(item().id()).fetchFirst().getUnchecked().get();
  }

  private static ImmutableItem item() {
    return ImmutableItem.builder()
            .id("1")
            .binary(Binary.create(new byte[] {1}))
            .build();
  }

}
