/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.immutables.common.tracing;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Delegating stub {@link LogManager} implementation.
 */
public class JdkLogManager extends LogManager {
  @Override
  public Logger getLogger(String name) {
    return new JdkLoggerAdapter(name);
  }

  @Override
  public synchronized Enumeration<String> getLoggerNames() {
    return Collections.enumeration(Collections.<String>emptySet());
  }

  @Override
  public void readConfiguration() throws IOException, SecurityException {
  }

  @Override
  public void reset() throws SecurityException {
  }

  @Override
  public void checkAccess() throws SecurityException {
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) throws SecurityException {
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) throws SecurityException {
  }

  @Override
  public void readConfiguration(InputStream ins) throws IOException, SecurityException {
  }

  @Override
  public String getProperty(String name) {
    return null;
  }

  @Override
  public boolean addLogger(Logger logger) {
    return false;
  }
}
