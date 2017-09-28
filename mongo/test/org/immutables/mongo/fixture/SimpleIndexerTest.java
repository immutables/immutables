package org.immutables.mongo.fixture;


import com.google.common.base.Optional;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;

import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;

public class SimpleIndexerTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final ForIndexerRepository repository = new ForIndexerRepository(context.setup());

  @Test
  public void index1() throws Exception {
    repository.index().withId().ensure().getUnchecked();
    repository.index().withString().ensure().getUnchecked();
    repository.index().withLongValue().ensure().getUnchecked();
    repository.index().withDoubleValue().ensure().getUnchecked();
    repository.index().withDate().ensure().getUnchecked();
    repository.index().withOptional().ensure().getUnchecked();

    final ImmutableForIndexer doc = create();
    repository.insert(doc).getUnchecked();

    // simple check of index
    check(repository.find(repository.criteria().longValue(doc.longValue())).fetchAll().getUnchecked()).hasSize(1);
  }

  @Test
  public void index2() throws Exception {
    repository.index()
            .withId()
            .withString()
            .withIntValue()
            .withLongValue()
            .withDate()
            .withOptional()
            .ensure()
            .getUnchecked();

    final ImmutableForIndexer doc = create();
    repository.insert(doc).getUnchecked();

    // simple check of index
    check(repository.find(repository.criteria().longValue(doc.longValue())).fetchAll().getUnchecked()).hasSize(1);
  }

  @Test
  public void sameAttributeIndex_exception() throws Exception {
    try {
      repository.index()
              .withString()
              .withString()
              .ensure()
              .getUnchecked();
      fail("Indexing on the same field not allowed");
    } catch (Exception ignore) {
      // ok
    }

    try {
      repository.index()
              .withString()
              .withStringDesceding()
              .ensure()
              .getUnchecked();
      fail("both asc/desc on the same index not allowed");
    } catch (Exception ignore) {
      // ok
    }
  }

  private static ImmutableForIndexer create() {
    return ImmutableForIndexer.builder()
            .id("id1")
            .string("string1")
            .intValue(11)
            .longValue(33L)
            .doubleValue(33.33D)
            .optional("Hello")
            .date(new Date())
            .build();
  }

  @Mongo.Repository
  @Value.Immutable
  @Gson.TypeAdapters
  interface ForIndexer {
    @Mongo.Id
    String id();

    String string();

    int intValue();

    long longValue();

    double doubleValue();

    Optional<String> optional();

    Optional<Date> date();

  }

}
