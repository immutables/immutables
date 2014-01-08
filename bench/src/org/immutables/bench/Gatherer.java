/*
    Copyright 2013-2014 Immutables.org authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.bench;

import com.google.common.base.Stopwatch;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class Gatherer {

  private static final int STATS_QUEUE_CAPACITY = 2000;

  private final AtomicInteger processedRequestCount = new AtomicInteger();

  private volatile ArrayBlockingQueue<ResponseStatistic> stats =
      new ArrayBlockingQueue<>(STATS_QUEUE_CAPACITY);

  int gatherAndResetProcessedCount() {
    return processedRequestCount.getAndSet(0);
  }

  Queue<ResponseStatistic> gatherAndResetStats() {
    ArrayBlockingQueue<ResponseStatistic> stats = this.stats;
    this.stats = new ArrayBlockingQueue<>(STATS_QUEUE_CAPACITY);
    return stats;
  }

  void requestProcessed(Stopwatch stopwatch, int responseArity) {
    processedRequestCount.incrementAndGet();

    stats.offer(
        new ResponseStatistic(
            stopwatch.elapsed(TimeUnit.MICROSECONDS),
            responseArity));
  }

  class ResponseStatistic {
    final long microseconds;
    final int responseArity;

    ResponseStatistic(long microseconds, int responseArity) {
      this.microseconds = microseconds;
      this.responseArity = responseArity;
    }
  }
}
