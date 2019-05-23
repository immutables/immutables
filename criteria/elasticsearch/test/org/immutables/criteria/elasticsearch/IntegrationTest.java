package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.immutables.check.Checkers.check;

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

  @BeforeClass
  public static void setUp() throws Exception {
    final ElasticsearchOps ops = new ElasticsearchOps(RESOURCE.restClient(), new ObjectMapper());

    Map<String, String> model = ImmutableMap.<String, String>builder()
            .put("string", "keyword")
            .put("optionalString", "keyword")
            .put("bool", "boolean")
            .put("intNumber", "integer")
            .build();

    ops.createIndex(INDEX_NAME, model);

    ObjectNode doc = MAPPER.createObjectNode();
    doc.put("string", "str");
    doc.put("optionalString", "opt");
    doc.put("bool", true);
    doc.put("intNumber", 42);
    ops.insertDocument(INDEX_NAME, doc);
  }

  @Test
  public void basic() throws Exception {
    ElasticsearchRepository<ElasticModel> repo = new ElasticsearchRepository<>(RESOURCE.restClient(),
            ElasticModel.class, MAPPER, INDEX_NAME);

    Observable<ElasticModel> obs = Observable.fromPublisher(repo.query(ElasticModelCriteria.create()));

    TestObserver<ElasticModel> test = obs.test();

    test.awaitTerminalEvent(1, TimeUnit.SECONDS);
    test.assertComplete().assertValueCount(1);

    ElasticModel result = test.values().get(0);

    check(result.string()).is("str");
    check(result.intNumber()).is(42);

  }
}
