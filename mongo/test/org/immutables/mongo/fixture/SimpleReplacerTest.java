package org.immutables.mongo.fixture;

import org.junit.Rule;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

public class SimpleReplacerTest {

  @Rule
  public final MongoContext context = new MongoContext();

  private final EntityRepository repository = new EntityRepository(context.setup());

  @Test
  public void findAndReplace() throws Exception {
    final ImmutableEntity entity = ImmutableEntity.builder().id("e1").version(0).value("v0").build();

    repository.upsert(entity).getUnchecked();

    // first upsert
    repository.find(repository.criteria().id(entity.id()).version(entity.version()))
            .andReplaceFirst(entity.withVersion(1).withValue("v1"))
            .upsert()
            .getUnchecked();

    check(findById("e1").version()).is(1);
    check(findById("e1").value()).isOf("v1");

    // second upsert
    repository.find(repository.criteria().id(entity.id()).version(1))
            .andReplaceFirst(entity.withVersion(2).withValue("v2"))
            .upsert()
            .getUnchecked();

    check(findById("e1").version()).is(2);
    check(findById("e1").value()).isOf("v2");

    // now try to update version which doesn't exists (v0). should return null (absent)
    check(repository.find(repository.criteria().id(entity.id()).version(0))
              .andReplaceFirst(entity.withVersion(33).withValue("v33"))
              .update()
              .getUnchecked()).isAbsent();

    // last version is 2
    check(findById("e1").version()).is(2);
  }


  @Test
  public void updateUpsert_when_empty() throws Exception {
    ImmutableEntity entity = ImmutableEntity.builder().id("e1").version(0).value("v1").build();

    check(repository.find(repository.criteria().id("missing").version(0))
            .andReplaceFirst(entity)
            .update()
            .getUnchecked()).isAbsent();

    check(repository.find(repository.criteria().id(entity.id()))
            .andReplaceFirst(entity)
            .returningNew()
            .upsert()
            .getUnchecked()).isOf(entity);

    check(repository.findAll().fetchAll().getUnchecked()).hasSize(1);

  }

  private Entity findById(String id) {
    return repository.findById(id).fetchFirst().getUnchecked().get();
  }


}
