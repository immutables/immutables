package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.Repository;
import org.immutables.criteria.constraints.Expressional;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Queries <a href="https://www.elastic.co/">ElasticSearch</a> data-store.
 * @param <T>
 */
public class ElasticsearchRepository<T> implements Repository<T> {

  private final RestClient restClient;
  private final ObjectMapper mapper;
  private final Class<T> type;
  private final String index;

  public ElasticsearchRepository(RestClient restClient,
                                 Class<T> type,
                                 ObjectMapper mapper,
                                 String index) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.type = Objects.requireNonNull(type, "type");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.index = Objects.requireNonNull(index, "index");
  }

  @Override
  public Publisher<T> query(Expressional<T> expressional) {
    Objects.requireNonNull(expressional, "expressional");
    try {
      return queryInternal(expressional);
    } catch (Exception e) {
      return Publishers.error(e);
    }
  }

  private Publisher<T> queryInternal(Expressional<T> expressional) throws Exception {
    final Request request = new Request("POST", String.format("/%s/_search", index));
    final String json = Elasticsearch.toQuery(expressional.expression());
    request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

    return Publishers.map(new AsyncRestPublisher(restClient, request), converter());
  }

  private Function<Response, T> converter() {
    return response -> {
      try (InputStream is = response.getEntity().getContent()) {
        final ObjectNode root = mapper.readValue(is, ObjectNode.class);
        final ArrayNode array = (ArrayNode) root.get("hits").get("hits");
        return mapper.treeToValue(array.get(0).get("_source"), type);
      } catch (IOException e) {
        final String message = String.format("Couldn't parse HTTP response %s into %s", response, type);
        throw new UncheckedIOException(message, e);
      }
    };
  }

}
