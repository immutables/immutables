/*
 * Copyright 2020 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.geode;

import com.google.common.collect.ImmutableSet;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class to convert java objects to
 * <a href="https://geode.apache.org/docs/guide/19/developing/query_additional/literals.html">Geode OQL literals</a> like dates, iterables, numbers etc.
 * Using bind variables is preferred however in some circumstances like
 * <a href="https://geode.apache.org/docs/guide/19/developing/continuous_querying/chapter_overview.html">Continuous Querying</a>
 * it is not possible.
 *
 * @see <a href="https://geode.apache.org/docs/guide/19/developing/continuous_querying/implementing_continuous_querying.html">Implementing Continuous Querying</a>
 */
final class OqlLiterals {

  private OqlLiterals() {}

  /**
   * Convert java object to <a href="https://geode.apache.org/docs/guide/19/developing/query_additional/literals.html">Geode OQL literal</a>
   */
  static String fromObject(Object value) {
    if (value == null) {
      return Objects.toString(null);
    } else if (value instanceof CharSequence) {
      return charSequence((CharSequence) value);
    } else if (value instanceof Pattern) {
      return pattern((Pattern) value);
    } else if (value instanceof Iterable) {
      return iterable((Iterable<?>) value);
    } else if (value.getClass().isEnum()) {
      return fromEnum((Enum<?>) value);
    } else if (value instanceof LocalDate) {
      return localDate((LocalDate) value);
    } else if (value instanceof LocalDateTime) {
      return localDateTime((LocalDateTime) value);
    } else if (value instanceof Instant) {
      return instant((Instant) value);
    } else if (value instanceof Date) {
      return date((Date) value);
    } else if (value instanceof Long) {
      return longValue((Long) value);
    }

    // probably string is best representation in OQL (without bind variables)
    return Objects.toString(value);
  }

  private static String iterable(Iterable<?> value) {
    Set<Object> set = ImmutableSet.copyOf(value);
    String asString = set.stream().map(OqlLiterals::fromObject).collect(Collectors.joining(", "));
    return "SET(" + asString + ")";
  }

  private static String pattern(Pattern pattern) {
    return charSequence(pattern.pattern());
  }

  private static String charSequence(CharSequence string) {
    return "'" + escape(string) + "'";
  }

  private static String localDate(LocalDate localDate) {
    return String.format("to_date('%s', '%s')", DateTimeFormatter.ISO_DATE.format(localDate), "yyyy-MM-dd");
  }

  private static String localDateTime(LocalDateTime localDateTime) {
    return instant(localDateTime.toInstant(ZoneOffset.UTC));
  }

  private static String instant(Instant instant) {
    // warning: java.util.Date doesn't have nano-second precision as Instant
    return date(Date.from(instant));
  }

  private static String date(Date date) {
    // convert to ISO format
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    return String.format("to_date('%s', '%s')", format.format(date), format.toPattern());
  }

  private static String longValue(Long value) {
    return value.toString() + 'L';
  }

  /**
   * convert enum value to string representation which is name of enum
   * @see Enum#name()
   */
  private static String fromEnum(Enum<?> value) {
    return charSequence(value.name());
  }

  /**
   * Replace single quote {@code '} with two quotes {@code ''}
   *
   * <p>From <a href="https://www.postgresql.org/docs/9.1/sql-syntax-lexical.html">SQL syntax in PostgreSQL</a>:
   *  <pre>
   *    To include the escape character in the identifier literally, write it twice.
   *  </pre>
   * </p>
   * @param oql string to escape
   * @return escaped string
   * @see
   */
  static String escape(CharSequence oql) {
    return oql.toString().replace("'", "''");
  }
}
