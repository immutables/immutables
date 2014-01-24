package org.immutables.bench;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractIdleService;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.Nullable;
import javax.inject.Inject;

class Bombardier extends AbstractIdleService {

  private final ScheduledExecutorService executor;
  private final BenchmarkConfiguration configuration;
  private final List<ScheduledFuture<?>> scheduledFutures = Lists.newArrayList();
  private final BlockingQueue<Runnable> executionQueue;
  private final int executionRepeatCount;
  private final int executionRandomLimit;
  private final Iterable<Callable<Integer>> callablesStream;

  private Iterator<Runnable> generateIterator;
  private final Gatherer gatherer;

  @Inject
  Bombardier(
      ScheduledExecutorService executor,
      BenchmarkConfiguration configuration,
      Iterable<Callable<Integer>> callablesStream,
      Gatherer gatherer) {
    this.callablesStream = callablesStream;
    this.executor = executor;
    this.configuration = configuration;
    this.gatherer = gatherer;
    this.executionQueue = new ArrayBlockingQueue<>(configuration.queueCapacity());
    this.executionRepeatCount = configuration.executionRepeatCount();
    this.executionRandomLimit = configuration.executionRandomLimit();
  }

  private final Runnable executeRunnable = new Runnable() {
    @Override
    public void run() {
      int count = repeateExecutionsCount();
      for (int j = 0; j < count; j++) {
        @Nullable
        Runnable requestExecution = executionQueue.poll();
        if (requestExecution == null) {
          break;
        }
        requestExecution.run();
      }
    }

    int repeateExecutionsCount() {
      return executionRepeatCount + (int) (Math.random() * executionRandomLimit);
    }
  };

  private final Runnable enqueRunnable = new Runnable() {
    @Override
    public void run() {
      while (executionQueue.offer(generateIterator.next())) {
      }
    }
  };

  private final Function<Callable<Integer>, Runnable> gatherRequestTransformer =
      new Function<Callable<Integer>, Runnable>() {
        @Override
        public Runnable apply(final Callable<Integer> callable) {
          return new Runnable() {
            @Override
            public void run() {
              Stopwatch stopwatch = Stopwatch.createStarted();
              int responseArity;
              try {
                responseArity = callable.call();
              } catch (Exception ex) {
                responseArity = 0;
              }
              gatherer.requestProcessed(stopwatch, responseArity);
            }
          };
        }
      };

  @Override
  protected void startUp() throws Exception {
    this.generateIterator = Iterables.transform(callablesStream, gatherRequestTransformer).iterator();

    for (int i = 0; i < configuration.workers(); i++) {
      scheduledFutures.add(
          configuration.executeSchedule()
              .schedule(executor, executeRunnable));
    }
    scheduledFutures.add(
        configuration.enqueSchedule()
            .schedule(executor, enqueRunnable));
  }

  @Override
  protected void shutDown() throws Exception {
    for (ScheduledFuture<?> future : scheduledFutures) {
      future.cancel(false);
    }
  }
}
