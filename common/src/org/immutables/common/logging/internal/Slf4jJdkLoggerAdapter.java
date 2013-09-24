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
package org.immutables.common.logging.internal;

import org.immutables.common.logging.Stringification;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

class Slf4jJdkLoggerAdapter extends Logger {

  private final org.slf4j.Logger log;

  Slf4jJdkLoggerAdapter(String name) {
    super(name, null);
    this.log = LoggerFactory.getLogger(name);
  }

  @Override
  public void addHandler(Handler handler) {}

  @Override
  public void removeHandler(Handler handler) {}

  @Override
  public void config(String msg) {
    log.debug(msg);
  }

  @Override
  public final boolean isLoggable(Level level) {
    return super.isLoggable(level);
  }

  @Override
  public void fine(String msg) {
    log.debug(msg);
  }

  @Override
  public void finer(String msg) {
    log.debug(msg);
  }

  @Override
  public Filter getFilter() {
    return null;
  }

  @Override
  public synchronized Handler[] getHandlers() {
    return new Handler[0];
  }

  @Override
  public Logger getParent() {
    return null;
  }

  @Override
  public boolean getUseParentHandlers() {
    return false;
  }

  @Override
  public void setFilter(Filter newFilter) throws SecurityException {}

  @Override
  public void setParent(Logger parent) {}

  @Override
  public void setLevel(Level newLevel) throws SecurityException {}

  @Override
  public synchronized void setUseParentHandlers(boolean useParentHandlers) {}

  @Override
  public Level getLevel() {
    return null;
  }

  @Override
  public void log(LogRecord record) {
    String message = formatRecord(record);
    if (record.getLevel() == Level.SEVERE) {
      log.error(message, record.getThrown());
    }
    else if (record.getLevel() == Level.WARNING) {
      log.warn(message, record.getThrown());
    }
    else if (record.getLevel() == Level.INFO) {
      log.info(message, record.getThrown());
    }
    else {
      log.trace(message, record.getThrown());
    }
  }

  private String formatRecord(LogRecord record) {
    String message = record.getMessage();
    Object[] parameters = record.getParameters();

    if (parameters == null || parameters.length == 0) {
      return message;
    }

    StringBuilder appendable = new StringBuilder();
    Stringification.appendMessage(appendable, null, message, parameters);
    return appendable.toString();
  }

  @Override
  public void severe(String msg) {
    log.error(msg);
  }

  @Override
  public void finest(String msg) {
    log.debug(msg);
  }

  @Override
  public void info(String msg) {
    log.info(msg);
  }

  @Override
  public void warning(String msg) {
    log.warn(msg);
  }
}
