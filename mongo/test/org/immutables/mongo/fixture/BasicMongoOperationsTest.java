package org.immutables.mongo.fixture;

import org.immutables.mongo.types.Binary;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.immutables.check.Checkers.check;

/**
 * Basic CRUD operations on the top of repository
 */
public class BasicMongoOperationsTest {

    @Rule
    public final MongoContext context = new MongoContext();

    private ItemRepository repository;

    @Before
    public void setUp() throws Exception {
        repository = new ItemRepository(context.setup());
    }

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

        Item item2 = items.get(0);

        check(item2.id()).is("1");

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
