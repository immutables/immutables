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

import org.immutables.check.StringChecker;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

import static org.immutables.check.Checkers.check;

class OqlLiteralsTest {
  @Test
  void escape() {
    check(OqlLiterals.escape("test")).is("test");
    check(OqlLiterals.escape("'")).is("''");
    check(OqlLiterals.escape("O'Hare")).is("O''Hare");
    check(OqlLiterals.escape("'test'")).is("''test''");
    check(OqlLiterals.escape("")).is("");
    check(OqlLiterals.escape("1")).is("1");
    check(OqlLiterals.escape("a")).is("a");
  }

  @Test
  void string() {
    literalOf("null").is("'null'");
    literalOf("").is("''");
    literalOf("a").is("'a'");
    literalOf("aa").is("'aa'");
  }

  @Test
  void longValue() {
    literalOf(Long.MAX_VALUE).is(Long.MAX_VALUE + "L");
    literalOf(1L).is("1L");
    literalOf(0L).is("0L");
    literalOf(-1L).is("-1L");
  }

  @Test
  void integer() {
    literalOf(1).is("1");
    literalOf(0).is("0");
    literalOf(-1).is("-1");
  }

  @Test
  void bool() {
    literalOf(true).is(Boolean.TRUE.toString());
    literalOf(false).is(Boolean.FALSE.toString());
  }

  @Test
  void nulls() {
    literalOf(null).is("null");
  }

  @Test
  void enums() {
    literalOf(Foo.A).is("'A'");
    literalOf(Foo.B).is("'B'");
  }

  @Test
  void javaUtilDate() throws ParseException {
    String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
    SimpleDateFormat format = new SimpleDateFormat(pattern);
    Date value = format.parse("2020-01-22 23:00:00.000");
    literalOf(value).is("to_date('2020-01-22 23:00:00.000', 'yyyy-MM-dd HH:mm:ss.SSS')");
  }

  @Test
  void localDate() {
    LocalDate date = LocalDate.of(2020, 12, 11);
    literalOf(date).is("to_date('2020-12-11', 'yyyy-MM-dd')");
  }

  private enum Foo {
    A, B;

    @Override
    public String toString() {
      return "INVALID:" + name();
    }
  }

  private static StringChecker literalOf(Object value) {
    return check(OqlLiterals.fromObject(value));
  }
}