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
/**
 * The logging is facade for usage of high-level semantic event logging as opposed to tracing-like
 * logging (log4j and followers). Instead of plumbing all kind of logging into low level framework,
 * {@code org.immutables.common.logging} provides simple, yet powerful log event abstraction. Framework include backend
 * interfaces like {@link org.immutables.service.logging.LogEventDispatcher}
 * and {@link org.immutables.service.logging.LogEventListener}, and also provides high
 * level consumer API based on proxying interfaces that define logging events via annotated method
 * declarations.
 */
@javax.annotation.ParametersAreNonnullByDefault
package org.immutables.service.logging;