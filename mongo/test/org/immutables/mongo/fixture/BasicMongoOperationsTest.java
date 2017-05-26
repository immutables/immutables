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

import java.util.List;
import org.immutables.mongo.types.Binary;
import org.junit.Rule;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

/**
 * Basic CRUD operations on the top of repository
 */
public class BasicMongoOperationsTest {

  @Rule
  public final MongoContext context = new MongoContext();

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
  public void delete() throws Exception {
    Item item = ImmutableItem.builder()
        .id("1")
        .binary(Binary.create(new byte[0]))
        .build();

    check(repository.insert(item).getUnchecked()).is(1);
    // expect single entry to be deleted
    check(repository.findAll().deleteAll().getUnchecked()).is(1);
    // second time no entries remaining
    check(repository.findAll().deleteAll().getUnchecked()).is(0);
  }
}
