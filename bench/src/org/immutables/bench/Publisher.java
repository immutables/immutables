package org.immutables.bench;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Publisher {

  private final Logger statsLogger;
  private final Logger distributionLogger;
  private final Logger csvLogger;

  Publisher(String loggerName) {
    this.statsLogger = LoggerFactory.getLogger(loggerName + ".stats");
    this.csvLogger = LoggerFactory.getLogger(loggerName + ".csv");
    this.distributionLogger = LoggerFactory.getLogger(loggerName + ".distribution");
  }

  void csv(String string) {
    csvLogger.info(string);
  }

  void distribution(String string) {
    distributionLogger.info(string);
  }

  public void stats(String string) {
    statsLogger.info(string);
  }
}
