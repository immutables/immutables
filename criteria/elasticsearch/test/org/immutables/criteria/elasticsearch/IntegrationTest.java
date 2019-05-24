package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Start embedded ES instance. Insert document(s) then query it.
 */
public class IntegrationTest {

  @ClassRule
  public static final EmbeddedElasticsearchResource RESOURCE = EmbeddedElasticsearchResource.create();

  private static final ObjectMapper MAPPER = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module());

  private static final String INDEX_NAME = "mymodel";

  private ElasticsearchRepository<ElasticModel> repository;

  @BeforeClass
  public static void setupElastic() throws Exception {
    final ElasticsearchOps ops = new ElasticsearchOps(RESOURCE.restClient(), new ObjectMapper());

    Map<String, String> model = ImmutableMap.<String, String>builder()
            .put("string", "keyword")
            .put("optionalString", "keyword")
            .put("bool", "boolean")
            .put("intNumber", "integer")
            .build();

    ops.createIndex(INDEX_NAME, model);

    ObjectNode doc1 = MAPPER.createObjectNode()
                .put("string", "foo")
                .put("optionalString", "optFoo")
                .put("bool", true)
                .put("intNumber", 42);

    ObjectNode doc2 = MAPPER.createObjectNode()
                .put("string", "bar")
                .put("optionalString", "optBar")
                .put("bool", false)
                .put("intNumber", 44);

    ops.insertBulk(INDEX_NAME, Arrays.asList(doc1, doc2));
  }

  @Before
  public void setupRepository() throws Exception {
    repository = new ElasticsearchRepository<>(RESOURCE.restClient(),
            ElasticModel.class, MAPPER, INDEX_NAME);

  }

  @Test
  public void criteria() {
    ElasticModelCriteria<ElasticModelCriteria.Self> crit = ElasticModelCriteria.create();

    assertCount(crit, 2);
    assertCount(crit.intNumber.isEqualTo(1), 0);
    assertCount(crit.string.isEqualTo("foo"), 1);
    assertCount(crit.string.isEqualTo("bar"), 1);
    assertCount(crit.string.isEqualTo("hello"), 0);

  }

  private void assertCount(ElasticModelCriteria<?> crit, int count) {
    Observable.fromPublisher(repository.query(crit))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(count);

  }

}
