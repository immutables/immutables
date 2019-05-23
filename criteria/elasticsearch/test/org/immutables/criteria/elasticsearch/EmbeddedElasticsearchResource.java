package org.immutables.criteria.elasticsearch;

import com.google.common.io.Closer;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.transport.TransportAddress;
import org.junit.rules.ExternalResource;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Used to initialize a single elastic node. For performance reasons (node startup costs),
 * same instance is usually shared across multiple tests.
 *
 * <p>This rule should be used as follows:
 * <pre>
 *  public class MyTest {
 *    &#64;ClassRule
 *    public static final EmbeddedElasticsearchResource RULE = EmbeddedElasticsearchResource.create();
 *
 *    &#64;BeforeClass
 *    public static void setup() {
 *       // ... populate instance
 *    }
 *
 *    &#64;Test
 *    public void myTest() {
 *      RestClient client = RULE.restClient();
 *      // ....
 *    }
 *  }
 *  </pre>
 * @see ExternalResource
 */
class EmbeddedElasticsearchResource extends ExternalResource implements Closeable {

  private final EmbeddedElasticsearchNode node;
  private final Closer closer;
  private RestClient client;

  private EmbeddedElasticsearchResource(EmbeddedElasticsearchNode node) {
    this.node = Objects.requireNonNull(node, "node");
    this.closer = Closer.create();
    closer.register(this.node);
  }

  @Override protected void before() throws Throwable {
    node.start();
  }

  @Override protected void after() {
    try {
      close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    closer.close();
  }

  /**
   * Factory method to create this rule.
   * @return managed resource to be used in unit tests
   */
  public static EmbeddedElasticsearchResource create() {
    return new EmbeddedElasticsearchResource(EmbeddedElasticsearchNode.create());
  }

  /**
   * Low-level http rest client connected to current embedded elastic search instance.
   * @return http client connected to ES cluster
   */
  RestClient restClient() {
    if (client != null) {
      return client;
    }

    final RestClient client = RestClient.builder(httpHost()).build();
    closer.register(client);
    this.client = client; // cache
    return client;
  }

  private HttpHost httpHost() {
    final TransportAddress address = node.httpAddress();
    return new HttpHost(address.getAddress(), address.getPort());
  }

}