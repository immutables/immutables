package org.immutables.mongo.fixture;


import com.google.common.base.Optional;
import java.util.Date;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;
import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;

@Gson.TypeAdapters
public class SimpleIndexerTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final ForIndexerRepository repository = new ForIndexerRepository(context.setup());

  @Test
  public void index1() throws Exception {
    repository.index().withId().named("id").ensure().getUnchecked();
    repository.index().withString().named("string").unique().expireAfterSeconds(2).ensure().getUnchecked();
    repository.index().withLongValue().named("longValue").expireAfterSeconds(3).ensure().getUnchecked();
    repository.index().withDoubleValue().named("doubleValue").ensure().getUnchecked();
    repository.index().withDateDesceding().named("date").ensure().getUnchecked();
    repository.index().withOptional().named("optional").ensure().getUnchecked();

    ImmutableForIndexer doc = create();
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

    ImmutableForIndexer doc = create();
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
    } catch (Exception e) {
      check(e.getMessage()).contains("string");
    }

    try {
      repository.index()
              .withString()
              .withStringDesceding()
              .ensure()
              .getUnchecked();
      fail("both asc/desc on the same index not allowed");
    } catch (Exception e) {
      check(e.getMessage()).contains("string");
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
