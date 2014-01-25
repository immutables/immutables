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

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import java.lang.annotation.Annotation;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * The log event.
 */
@Beta
public interface LogEvent {

  /**
   * Gets the timestamp.
   * @return the timestamp
   */
  long getTimestamp();

  /**
   * Gets the severity.
   * @return the severity
   */
  Severity getSeverity();

  /**
   * Queries special meta-data for the following event
   * @param <A> the generic type
   * @param annotationType the annotation type
   * @return the optional annotation value
   */
  <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationType);

  /**
   * Details of event.
   * @return event details, may be empty string
   */
  String getDetails();

  /**
   * Event descriptive code. Together with {@link #getSourceCategory()} uniquely identifies event.
   * @return the string
   */
  String getDescriptiveCode();

  /**
   * Event source is category. Is category of event, or more precisely subsystem or type of
   * activity where error occurred
   * @return the source category
   */
  String getSourceCategory();

  /**
   * Gets the message, using the desired locale if possible.
   * @param locale the locale, may be {@code null}
   * @return the message
   */
  String getMessage(@Nullable Locale locale);
}
