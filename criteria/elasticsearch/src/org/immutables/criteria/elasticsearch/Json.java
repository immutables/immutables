/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Mapping classes to elastic search result
 */
class Json {

  /**
   * Response from Elastic
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Result {
    private final SearchHits hits;
    private final String scrollId;
    private final long took;

    /**
     * Constructor for this instance.
     *
     * @param hits list of matched documents
     * @param took time taken (in took) for this query to execute
     */
    @JsonCreator
    Result(@JsonProperty("hits") SearchHits hits,
           @JsonProperty("_scroll_id") String scrollId,
           @JsonProperty("took") long took) {
      this.hits = Objects.requireNonNull(hits, "hits");
      this.scrollId = scrollId;
      this.took = took;
    }

    SearchHits searchHits() {
      return hits;
    }

    Duration took() {
      return Duration.ofMillis(took);
    }

    Optional<String> scrollId() {
      return Optional.ofNullable(scrollId);
    }

    public boolean isEmpty() {
      return searchHits().hits().isEmpty();
    }

  }

  /**
   * Similar to {@code SearchHits} in ES. Container for {@link SearchHit}
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class SearchHits {

    private final List<SearchHit> hits;

    @JsonCreator
    SearchHits(@JsonProperty("hits") final List<SearchHit> hits) {
      this.hits = Objects.requireNonNull(hits, "hits");
    }

    public List<SearchHit> hits() {
      return this.hits;
    }
  }

  /**
   * Concrete result record which matched the query. Similar to {@code SearchHit} in ES.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class SearchHit {

    /**
     * ID of the document (not available in aggregations)
     */
    private final String id;
    private final ObjectNode source;

    @JsonCreator
    SearchHit(@JsonProperty("_id") final String id,
              @JsonProperty("_source") final ObjectNode source) {
      this.id = Objects.requireNonNull(id, "id");

      this.source = source;
    }

    /**
     * Returns id of this hit (usually document id)
     *
     * @return unique id
     */
    public String id() {
      return id;
    }

    public ObjectNode source() {
      return source;
    }
  }

}
