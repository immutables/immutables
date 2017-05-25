package org.immutables.mongo.fixture;

import org.bson.types.ObjectId;
import org.immutables.mongo.types.Id;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.immutables.check.Checkers.check;

public class SimpleUpdaterTest {

  @Rule
  public final MongoContext context = new MongoContext();

  private final ItemRepository repository = new ItemRepository(context.setup());

  /**
   * Checks {@code $set} functionality of the repository
   */
  @Test
  public void setList() throws Exception {
    final String id = "i1";

    check(overrideList(id, Collections.singleton("BAD"))).is(0);

    ImmutableItem item = ImmutableItem.builder().id(id).addList("l1").build();

    check(repository.upsert(item).getUnchecked()).is(1);
    check(findById(id).list()).hasAll("l1");

    overrideList(id, Collections.<String>emptyList());
    check(findById(id).list()).isEmpty();

    overrideList(id, Collections.singleton("l2"));
    check(findById(id).list()).hasAll("l2");

    overrideList(id, Arrays.asList("l3", "l4", "l5"));
    check(findById(id).list()).hasAll("l3", "l4", "l5");
  }

  @Test
  public void set_with_other_operations() throws Exception {
    final String id = "i1";

    ImmutableItem item = ImmutableItem.builder().id(id).addList("l1").build();

    check(repository.upsert(item).getUnchecked()).is(1);

    repository.update(repository.criteria().id(id))
            .setTags(Collections.singleton(ImmutableTag.of("t1")))
            .setList(Collections.singleton("l1"))
            .setIds(Collections.singleton(Id.fromString(new ObjectId().toString())))
            .updateFirst()
            .getUnchecked();

    final Item item2 = findById(id);

    check(item2.list()).hasAll("l1");
    check(item2.tags()).hasAll(ImmutableTag.of("t1"));
    check(item2.ids()).hasSize(1);
  }

  /**
   * {@code $set} functionality but on a non-scalar Object
   */
  @Test
  public void setList_non_scalar_object() throws Exception {
    final String id = "i1";
    ImmutableItem item = ImmutableItem.builder().id(id).addList("l1").addTags(ImmutableTag.of("t1")).build();

    check(repository.upsert(item).getUnchecked()).is(1);

    repository.update(repository.criteria().id(id)).setTags(Collections.<Item.Tag>emptyList()).updateFirst().getUnchecked();

    check(findById(id).tags()).isEmpty();

    final Set<? extends Item.Tag> set1 = Collections.singleton(ImmutableTag.of("t2"));
    repository.update(repository.criteria().id(id))
            .setTags(set1)
            .updateFirst()
            .getUnchecked();

    check(findById(id).tags()).hasAll(set1);


    final List<? extends Item.Tag> set2 = Arrays.asList(ImmutableTag.of("t3"), ImmutableTag.of("t4"));

    repository.update(repository.criteria().id(id))
            .setTags(set2)
            .updateFirst()
            .getUnchecked();

    check(findById(id).tags()).hasAll(set2);
  }

  /**
   * Checks {@code $push} functionality of repository
   */
  @Test
  public void addToList() throws Exception {
    final String id = "i1";
    ImmutableItem item = ImmutableItem.builder().id(id).addList("l1").build();

    check(repository.upsert(item).getUnchecked()).is(1);

    push(id, Collections.singleton("l2"));
    check(findById(id).list()).hasAll("l1", "l2");

    push(id, Arrays.asList("l3", "l4"));
    check(findById(id).list()).hasAll("l1", "l2", "l3", "l4");
  }

  @Test
  public void clear() throws Exception {
    final String id = "i1";
    ImmutableItem item = ImmutableItem.builder().id(id).addList("l1").build();
    repository.upsert(item).getUnchecked();

    repository.update(repository.criteria().id(id))
            .clearList().updateFirst().getUnchecked();

    check(findById(id).list()).isEmpty();
  }

  private int push(String id, Iterable<String> values) {
    return repository.update(repository.criteria().id(id))
            .addAllList(values)
            .updateFirst()
            .getUnchecked();
  }

  private int overrideList(String id, Iterable<String> list) {
    return repository.update(repository.criteria().id(id))
            .setList(list)
            .updateFirst()
            .getUnchecked();
  }

  private Item findById(String id) {
    return repository.findById(id).fetchFirst().getUnchecked().get();
  }
}
