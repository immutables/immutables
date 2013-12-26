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

import org.immutables.service.logging.Logging.Details;
import org.immutables.service.logging.Logging.ERROR;
import org.immutables.service.logging.Logging.INFO;
import org.immutables.service.logging.Logging.Message;
import org.immutables.service.logging.Logging.WARNING;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.immutables.common.time.TimeInstantSource;

class LogSourceInterfaceHandler implements InvocationHandler {

  private final LogEventDispatcher publisher;

  LogSourceInterfaceHandler(LogEventDispatcher logPublisher) {
    this.publisher = logPublisher;
  }

  static <T> T proxyFor(Class<T> declarator, LogEventDispatcher dispatcher) {
    InvocationHandler handler = new LogSourceInterfaceHandler(dispatcher);

    Object proxy = Proxy.newProxyInstance(
        declarator.getClassLoader(),
        new Class<?>[] { declarator }, handler);

    return declarator.cast(proxy);
  }

  @Override
  public Object invoke(Object proxy, final Method method, @Nullable final Object[] parameters)
      throws Throwable {

    return new EventDispatchFuture(publisher.post(new Callable<LogEvent>() {
      @Override
      public LogEvent call() throws Exception {
        return new MethodDefinedLogEvent(method, parameters);
      }
    }));
  }

  private class MethodDefinedLogEvent implements LogEvent {

    private final long timestamp = TimeInstantSource.systemSource().read();

    private final String eventDescriptiveCode;

    private final String messagePattern;

    private final Object[] inserts;

    private final Severity severity;

    private final String detail;

    private final String eventSource;

    private final Annotation[] annotations;

    MethodDefinedLogEvent(Method method, @Nullable Object[] methodParameters) {
      eventSource = method.getDeclaringClass().getSimpleName();
      eventDescriptiveCode = method.getName();
      severity = inferSeverity(method);
      annotations = method.getDeclaredAnnotations();
      messagePattern = extractMessagePattern(method);

      if (methodParameters != null && methodParameters.length != 0) {
        Object[] parameters = methodParameters;
        // Unbox single varargs parameter
        if (parameters.length == 1 && method.isVarArgs()) {
          parameters = (Object[]) parameters[0];
        }

        detail = extractDetails(method, parameters);
        inserts = parameters;
      }
      else {
        detail = "";
        inserts = null;
      }
    }

    private String extractDetails(Method method, Object[] parameters) {
      int lastIndex = parameters.length - 1;
      Object lastParameter = parameters[lastIndex];

      if (detailParameterAnnotationFound(method, lastIndex)) {
        // Replacing detail parameter with empty string. To not break any processing
        parameters[lastIndex] = "";

        if (lastParameter instanceof Throwable) {
          return Throwables.getStackTraceAsString((Throwable) lastParameter);
        }

        return Stringification.stringify(lastParameter, "");
      }

      if (lastParameter instanceof Throwable) {
        Throwable throwable = (Throwable) lastParameter;
        parameters[lastIndex] = throwable.toString();
        return Throwables.getStackTraceAsString(throwable);
      }

      return "";
    }

    private boolean detailParameterAnnotationFound(Method method, int parameterIndex) {
      Annotation[][] parameterAnnotations = method.getParameterAnnotations();

      if (parameterAnnotations.length > parameterIndex) {
        for (Annotation a : parameterAnnotations[parameterIndex]) {
          if (a instanceof Details) {
            return true;
          }
        }
      }

      return false;
    }

    private String extractMessagePattern(Method method) {
      if (method.isAnnotationPresent(Message.class)) {
        return method.getAnnotation(Message.class).value();
      }

      return eventDescriptiveCode;
    }

    private Severity inferSeverity(Method method) {
      Class<?> type = method.getReturnType();

      if (type == WARNING.class) {
        return Severity.WARNING;
      }

      if (type == ERROR.class) {
        return Severity.ERROR;
      }

      return Severity.INFO;
    }

    @Override
    public String getDescriptiveCode() {
      return eventDescriptiveCode;
    }

    @Override
    public String getMessage(Locale locale) {
      if (locale == null) {
        return formatCodeWithInserts();
      }

      if (inserts != null) {
        StringBuilder builder = new StringBuilder();
        Stringification.appendMessage(builder, locale, messagePattern, inserts);
        return builder.toString();
      }

      return messagePattern;
    }

    private String formatCodeWithInserts() {
      return eventSource + "." + eventDescriptiveCode +
          Stringification.stringify(inserts, "[]").replace('[', '(').replace(']', ')');
    }

    @Override
    public Severity getSeverity() {
      return severity;
    }

    @Override
    public String getSourceCategory() {
      return eventSource;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public String getDetails() {
      return detail;
    }

    @Override
    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationType) {
      for (Annotation a : annotations) {
        if (annotationType.isInstance(a)) {
          return Optional.of(annotationType.cast(a));
        }
      }
      return Optional.absent();
    }

    @Override
    public String toString() {
      return getMessage(null);
    }
  }

  static class EventDispatchFuture implements ERROR, WARNING, INFO {

    final Future<?> dispatchFuture;

    EventDispatchFuture(Future<?> broadcastFuture) {
      this.dispatchFuture = broadcastFuture;
    }

    @Override
    public boolean awaitDispatch() {
      try {
        this.dispatchFuture.get();
        return true;
      } catch (Throwable e) {
        Logging.LOGGER.error("Log event dispatch failed", Throwables.getRootCause(e));
      }
      return false;
    }
  }
}
