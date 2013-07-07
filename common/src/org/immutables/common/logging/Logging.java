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
package org.immutables.common.logging;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Formatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The logging is facade for usage of high-level semantic event logging as opposed to tracing-like
 * logging (log4j and followers). Instead of plumbing all kind of logging into low level framework,
 * {@link Logging} provides simple, yet powerful log event abstraction. Framework include backend
 * interfaces like {@link LogEventDispatcher} and {@link LogEventListener}, and also provides high
 * level consumer API based on proxying interfaces that define logging events via annotated method
 * declarations.
 */
// XXX May need to get rid of statics and configure in container
public final class Logging {

  static final Logger LOGGER = LoggerFactory.getLogger(Logging.class);

  private static final int MAX_RECENT_EVENTS = 20;

  private static final ExecutorService DISPATCH_EXECUTOR =
      Executors.newSingleThreadExecutor(
          new ThreadFactoryBuilder()
              .setDaemon(true)
              .setNameFormat("tw.technology.logging")
              .build());

  private static final LogEventDispatcher DISPATCHER =
      new DefaultEventLogDispatcher(DISPATCH_EXECUTOR, MAX_RECENT_EVENTS);

  private Logging() {
  }

  /**
   * Gets the log event dispatcher.
   * @return the log event dispatcher
   */
  public static LogEventDispatcher dispatcher() {
    return DISPATCHER;
  }

  /**
   * Creates the logger proxy.
   * @param <T> the generic type
   * @param declarator the interface that has methods defining log events
   * @return the t
   */
  public static <T> T proxyFor(Class<T> declarator) {
    return proxyFor(declarator, DISPATCHER);
  }

  static <T> T proxyFor(Class<T> declarator, LogEventDispatcher dispatcher) {
    return LogSourceInterfaceHandler.proxyFor(declarator, dispatcher);
  }

  /**
   * Message format string. May contain printf placeholders. So event method arguments will be
   * used as substitution parameters.
   * @see Formatter
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface Message {

    /**
     * Message format string.
     */
    String value();
  }

  /**
   * Marks method parameter as content for event's details field. Applies only to last parameter.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public static @interface Details {
  }

  /**
   * {@link Severity#ERROR}.
   */
  public interface ERROR {

    /**
     * Await dispatch.
     * @return true, if successful
     */
    boolean awaitDispatch();
  }

  /**
   * {@link Severity#WARNING}.
   */
  public interface WARNING {

    /**
     * Await dispatch.
     * @return true, if successful
     */
    boolean awaitDispatch();
  }

  /**
   * {@link Severity#INFO}.
   */
  public interface INFO {

    /**
     * Await dispatch.
     * @return true, if successful
     */
    boolean awaitDispatch();
  }
}
