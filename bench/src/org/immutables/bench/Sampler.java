package org.immutables.bench;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

public class Sampler {

  private static final boolean SYSOUT_CSV = Boolean.getBoolean("sampler.sysout-csv");
  private static final String CSV_FILE_NAME = System.getProperty("sampler.csv-file-name", "log.csv");
  private static final int SAMPLE_STATS_INITIAL_DELAY = Integer.getInteger("sampler.stats.initial-delay-ms", 2000);
  private static final int SAMPLE_STATS_DELAY = Integer.getInteger("sampler.stats.delay-ms", 1000);
  private static final int SAMPLE_BUCKETS_DELAY = Integer.getInteger("sampler.buckets.delay-ms", 5000);
  private static final int SAMPLE_BUCKETS_INITIAL_DELAY = Integer.getInteger("sampler.buckets.initial-delay-ms", 7500);

  private static final long MICROS_IN_MS = TimeUnit.MILLISECONDS.toMicros(1);

  @Inject
  Gatherer gatherer;

  @Resource
  ScheduledExecutorService scheduledExecutor;

  private PrintWriter writer;

  @PostConstruct
  public void init() {
    scheduledExecutor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        summarizeStats();
      }
    }, SAMPLE_STATS_INITIAL_DELAY, SAMPLE_STATS_DELAY, TimeUnit.MILLISECONDS);

    scheduledExecutor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        summarizeBuckets();
      }
    }, SAMPLE_BUCKETS_INITIAL_DELAY, SAMPLE_BUCKETS_DELAY, TimeUnit.MILLISECONDS);

    if (SYSOUT_CSV) {
      try {
        writer = new PrintWriter(new FileWriter(CSV_FILE_NAME, false));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void summarizeBuckets() {
    double avgResponseMicros = 0;
    int count = 0;

    ArrayBlockingQueue<Long> avgs = this.perSecondAverages;
    this.perSecondAverages = new ArrayBlockingQueue<>(10);

    Long responseTime;
    while ((responseTime = avgs.poll()) != null) {
      count++;
      long t = responseTime;
      avgResponseMicros += t;
    }

    if (count == 0) {
      return;
    }

    printBuckets(avgResponseMicros, count);
  }

  private void printBuckets(double avgResponseMicros, int count) {
    double allBucketed = allBucketsResponses.get();

    System.err.printf(""
        + "RSP AVG %.2f ms\n"
        + "RSP BUCKETS 0   < 5  < 10 < 20 < 50 <100 <200 <300 <400 <inf\n"
        + "            %.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
        avgResponseMicros / MICROS_IN_MS / count,
        bucket0_5.get() / allBucketed,
        bucket5_10.get() / allBucketed,
        bucket10_20.get() / allBucketed,
        bucket20_50.get() / allBucketed,
        bucket50_100.get() / allBucketed,
        bucket100_200.get() / allBucketed,
        bucket200_300.get() / allBucketed,
        bucket300_400.get() / allBucketed,
        bucket400_inf.get() / allBucketed);
  }

  private volatile ArrayBlockingQueue<Long> perSecondAverages = new ArrayBlockingQueue<>(10);

  private final AtomicInteger bucket0_5 = new AtomicInteger();
  private final AtomicInteger bucket5_10 = new AtomicInteger();
  private final AtomicInteger bucket10_20 = new AtomicInteger();
  private final AtomicInteger bucket20_50 = new AtomicInteger();
  private final AtomicInteger bucket50_100 = new AtomicInteger();
  private final AtomicInteger bucket100_200 = new AtomicInteger();
  private final AtomicInteger bucket200_300 = new AtomicInteger();
  private final AtomicInteger bucket300_400 = new AtomicInteger();
  private final AtomicInteger bucket400_inf = new AtomicInteger();
  private final AtomicLong allBucketsResponses = new AtomicLong(1);

  private void summarizeStats() {
    int processed = gatherer.gatherAndResetProcessedCount();
    Queue<Gatherer.ResponseStatistic> stats = gatherer.gatherAndResetStats();

    long max = 0;
    long avg = 0;

    int count = 0;
    int hasResults = 0;

    Gatherer.ResponseStatistic stat;

    while ((stat = stats.poll()) != null) {
      count++;
      if (stat.responseArity >= 0) {
        hasResults++;
      }

      long t = stat.microseconds;
      avg += t;

      if (t > max) {
        max = t;
      }

      incrementBuckets(t);
    }

    if (count == 0) {
      return;
    }

    avg /= count;
    perSecondAverages.offer(avg);

    printStats(new Object[] {
        processed,
        avg / (double) MICROS_IN_MS,
        max / (double) MICROS_IN_MS,
        (int) ((hasResults / (double) count) * 100)
    });
  }

  private void incrementBuckets(long t) {
    long millis = t / MICROS_IN_MS;

    if (millis < 5) {
      bucket0_5.incrementAndGet();
    }
    else if (millis < 10) {
      bucket5_10.incrementAndGet();
    }
    else if (millis < 20) {
      bucket10_20.incrementAndGet();
    }
    else if (millis < 50) {
      bucket20_50.incrementAndGet();
    }
    else if (millis < 100) {
      bucket50_100.incrementAndGet();
    }
    else if (millis < 200) {
      bucket100_200.incrementAndGet();
    }
    else if (millis < 300) {
      bucket200_300.incrementAndGet();
    }
    else if (millis < 400) {
      bucket300_400.incrementAndGet();
    }
    else {
      bucket400_inf.incrementAndGet();
    }
    allBucketsResponses.incrementAndGet();
  }

  private void printStats(Object[] inserts) {
    System.err.printf("TPS %d, RSP AVG %.2f << %.2f ms \t\t( %d%% has result )\n", inserts);

    if (SYSOUT_CSV) {
      // System.out.printf("%d;%.2f;%.2f;%d\n", inserts);
      writer.printf("%d;%.2f;%.2f;%d\n", inserts);
      writer.flush();
    }
  }
}
