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

import org.immutables.service.logging.Logging;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.immutables.service.logging.Logging.Details;
import org.immutables.service.logging.Logging.ERROR;
import org.immutables.service.logging.Logging.INFO;
import org.immutables.service.logging.Logging.Message;
import org.immutables.service.logging.Logging.WARNING;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

interface TestLogEvents {

  @Message("cannot access host '%s' with port %d")
  ERROR cannotAccessSomeHost(String hostName, int port);

  INFO allSeemsToBeFineNow();

  @Message("(%d %.2f)")
  WARNING somethingStrangeGoingOn(Object... data);

  @Message("%s")
  ERROR unexpectedFailure(Throwable throwable);

  @Message("")
  INFO status(String report, Map<String, Integer> stateByUnit);

  @Message("")
  INFO statusDetail(String report, @Details Map<String, Integer> stateByUnit);

}

public class LoggingTest {

  DefaultEventLogDispatcher dispatcher = new DefaultEventLogDispatcher(MoreExecutors.sameThreadExecutor(), 10);
  TestLogEvents logProxy = Logging.proxyFor(TestLogEvents.class, dispatcher);

  /**
   * Log framework run.
   * @throws Exception the exception
   */
  @Test
  public void logFrameworkRun() throws Exception {
    final List<LogEvent> events = Lists.newArrayList();

    LogEventListener logListener = new LogEventListener() {
      @Override
      public void logEventPosted(LogEvent event) {
        events.add(event);
      }
    };

    logProxy.allSeemsToBeFineNow().awaitDispatch();

    // Registering after one of event fired to check registration routine
    dispatcher.register(logListener);

    logProxy.cannotAccessSomeHost("host.com", 42).awaitDispatch();
    logProxy.somethingStrangeGoingOn(1, 3.1415).awaitDispatch();

    check(events).hasSize(3);

    LogEvent event0 = events.get(0);
    LogEvent event1 = events.get(1);
    LogEvent event2 = events.get(2);

    for (LogEvent event : events) {
      check(event.getSourceCategory()).is(TestLogEvents.class.getSimpleName());
    }

    check(event0.getDescriptiveCode()).is("allSeemsToBeFineNow");
    check(event1.getDescriptiveCode()).is("cannotAccessSomeHost");
    check(event2.getDescriptiveCode()).is("somethingStrangeGoingOn");

    check(event0.getSeverity()).is(Severity.INFO);
    check(event1.getSeverity()).is(Severity.ERROR);
    check(event2.getSeverity()).is(Severity.WARNING);

    check(event0.getMessage(null)).is("TestLogEvents.allSeemsToBeFineNow()");
    check(event1.getMessage(null)).is("TestLogEvents.cannotAccessSomeHost(host.com, 42)");
    check(event1.getMessage(Locale.ROOT)).is("cannot access host 'host.com' with port 42");
    check(event2.getMessage(new Locale("ru"))).is("(1 3,14)");
    check(event2.getMessage(Locale.ENGLISH)).is("(1 3.14)");

    Logging.dispatcher().getListeners().remove(logListener);
  }

  /**
   * Detail boxing.
   * @throws Exception the exception
   */
  @Test
  public void detailBoxing() throws Exception {
    final Deque<LogEvent> events = Lists.newLinkedList();

    dispatcher.getListeners().add(new LogEventListener() {
      @Override
      public void logEventPosted(LogEvent event) {
        events.add(event);
      }
    });

    logProxy.unexpectedFailure(new Exception("Unbelivable!"));

    Map<String, Integer> stateByUnit = Maps.newHashMap();
    stateByUnit.put("A", 2);
    stateByUnit.put("B", 1);
    logProxy.status("...", stateByUnit).awaitDispatch();
    logProxy.statusDetail("...", stateByUnit).awaitDispatch();

    LogEvent event = events.remove();

    check(event.getMessage(Locale.ROOT)).is("java.lang.Exception: Unbelivable!");
    check(event.getDetails()).startsWith("java.lang.Exception: Unbelivable!");
    check(event.getDetails()).contains(getClass().getSimpleName());

    event = events.remove();

    check(event.getMessage(Locale.ROOT)).is("...{A=2, B=1}");
    check(event.getDetails()).isEmpty();

    event = events.remove();

    check(event.getMessage(Locale.ROOT)).is("...");
    check(event.getDetails()).is("{A=2, B=1}");
  }
}
