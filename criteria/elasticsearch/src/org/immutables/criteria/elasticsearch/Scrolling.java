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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import hu.akarnokd.rxjava2.operators.FlowableTransformers;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Uses <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html">Scroll API</a>
 * to fetch results recursively and asynchronously.
 */
class Scrolling<T> {

  private final ElasticsearchOps ops;

  private final Class<T> type;

  private final int scrollSize;

  /**
   * Duration to keep scroll context alive.
   */
  private final Duration keepAlive = Duration.ofMinutes(1);

  Scrolling(ElasticsearchOps ops, Class<T> type) {
    this.ops = ops;
    this.type = type;
    this.scrollSize = ops.scrollSize;
  }

  Flowable<T> scroll(ObjectNode query) {
    final Map<String, String> params = Collections.singletonMap("scroll", keepAlive.getSeconds() + "s");
    final boolean hasLimit = query.has("size");
    final long limit = hasLimit ? query.get("size").asLong() : Long.MAX_VALUE;

    if (scrollSize > limit) {
      // don't use scrolling when batch size is greater than limit
      return ops.search(query, type);
    }

    // override size during scrolling
    query.put("size", scrollSize);

    return ops.searchRaw(query, params)
            .toFlowable()
            .scan(AccumulatedResult.empty(limit), AccumulatedResult::next)
            .skip(1)// skip first empty accumulator
            .compose(transformer())
            .map(AccumulatedResult::result)
            .flatMapIterable(r -> r.searchHits().hits())
            .compose(f -> hasLimit ? f.limit(limit) : f) // limit number of elements
            .map(hit -> ops.jsonConverter(type).apply(hit.source()));
  }

  private FlowableTransformer<AccumulatedResult, AccumulatedResult> transformer() {
    return FlowableTransformers.expand(r -> r.isLast() ?
            maybeClearScroll(r.result).toFlowable() :
            ops.nextScroll(r.result.scrollId().get()).toFlowable().map(r::next));
  }

  /**
   * If scrollId is available will close scroll context
   */
  private Completable maybeClearScroll(Json.Result result) {
    Objects.requireNonNull(result, "result");
    return result.scrollId().map(scrollId -> ops.closeScroll(Collections.singleton(scrollId)))
            .orElse(Completable.complete());
  }

  /**
   * Used to track number of SearchHits seen so far (to know when to finish fetching)
   */
  private static class AccumulatedResult {

    /**
     * Number of documents fetched so far
     */
    private final long count;

    /**
     * Max number of documents to fetch
     */
    private final long limit;

    private final Json.Result result;

    private AccumulatedResult(long count, long limit, Json.Result result) {
      this.count = count;
      this.limit = limit;
      this.result = result;
    }

    boolean isLast() {
      Preconditions.checkState(result != null, "result not supposed to be null");
      return result.isEmpty() || count >= limit;
    }

    Json.Result result() {
      return result;
    }

    static AccumulatedResult empty(long limit) {
      return new AccumulatedResult(0, limit, null);
    }

    AccumulatedResult next(Json.Result next) {
      Objects.requireNonNull(next, "next");
      return new AccumulatedResult(count + next.searchHits().hits().size(), limit, next);
    }

  }

}
