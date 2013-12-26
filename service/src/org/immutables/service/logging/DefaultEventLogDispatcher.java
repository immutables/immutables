/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.service.logging;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.concurrent.GuardedBy;
import static com.google.common.base.Preconditions.*;

/**
 * The default log service.
 */
class DefaultEventLogDispatcher implements LogEventDispatcher {

  private final int recentEventsLimit;
  private final ExecutorService executor;

  private final Collection<LogEventListener> listeners = new CopyOnWriteArraySet<>();
  @GuardedBy("recentEvents")
  private final LinkedList<LogEvent> recentEvents = Lists.newLinkedList();

  DefaultEventLogDispatcher(ExecutorService executor, int maxRecentEvents) {
    this.executor = executor;
    this.recentEventsLimit = maxRecentEvents;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<LogEventListener> getListeners() {
    return listeners;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<LogEvent> recentLogEvents() {
    synchronized (recentEvents) {
      return ImmutableList.copyOf(recentEvents);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(final LogEventListener listener) {
    getListeners().add(listener);
    final List<LogEvent> missedEvents = recentLogEvents();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        for (LogEvent logEvent : missedEvents) {
          listener.logEventPosted(logEvent);
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<LogEvent> post(final Callable<LogEvent> eventCallaback) {
    checkNotNull(eventCallaback);
    try {
      return executor.submit(new Callable<LogEvent>() {
        @Override
        public LogEvent call() throws Exception {
          LogEvent logEvent = checkNotNull(eventCallaback.call());
          dispatchEvent(logEvent);
          return logEvent;
        }
      });
    } catch (RejectedExecutionException ex) {
      return Futures.immediateCancelledFuture();
    }
  }

  private synchronized void dispatchEvent(LogEvent currentEvent) {
    addRecent(currentEvent);

    for (LogEventListener listener : listeners) {
      try {
        listener.logEventPosted(currentEvent);
      } catch (Throwable th) {
        Logging.LOGGER.error("Log event listener invocation failed on " + currentEvent, th);
      }
    }
  }

  private void addRecent(LogEvent logEvent) {
    if (recentEventsLimit > 0) {
      synchronized (recentEvents) {
        recentEvents.addLast(logEvent);
        while (recentEvents.size() > recentEventsLimit) {
          recentEvents.removeFirst();
        }
      }
    }
  }
}
