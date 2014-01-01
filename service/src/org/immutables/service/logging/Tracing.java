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

/**
 */
public final class Tracing {
  public static void init() {
    // Trying to call as early as possible before any library tries to use JDK logging with default
    // log manager. But anyway some kind of laucher should also reference and trigger this class
    // initialisation in the early beginning. For the same reason class specified as string
    // rather than 'Literal.class.getName()'
    System.setProperty("java.util.logging.manager", "org.immutables.common.logging.internal.Slf4jJdkLogManager");
  }
}
