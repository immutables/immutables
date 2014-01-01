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
package org.immutables.service.logging;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * The log event dispatcher service.
 */
public interface LogEventDispatcher {

  /**
   * Post an event generation callback to dispatch.
   * @param eventCallaback the event callaback
   * @return future of when dispatch will happen
   */
  Future<LogEvent> post(Callable<LogEvent> eventCallaback);

  /**
   * Get adjustable collection of listeners.
   * @return mutable collection of listeners
   */
  Collection<LogEventListener> getListeners();

  /**
   * Recent log events. Ordered by insertion order.
   * @return the iterable that is snapshop of N recent events
   */
  List<LogEvent> recentLogEvents();

  /**
   * Registers listener and pushes all recent events to it.
   * @param listener the listener
   */
  void register(LogEventListener listener);
}
