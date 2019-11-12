/*
 * Copyright 2019 Immutables Authors and Contributors
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
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Path;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decorates existing {@link PathNaming} by escaping geode reserved words within double quotes {@code "type"}.
 * @see <a href="https://geode.apache.org/docs/guide/19/developing/querying_basics/reserved_words.html">Reserved Words in Geode</a>
 */
class ReservedWordNaming implements PathNaming  {

  /**
   * List of reserved keywords in Geode. They have to be escaped (surrounded) with double quotation marks.
   * @see <a href="https://geode.apache.org/docs/guide/19/developing/querying_basics/reserved_words.html">Reserved Words in Geode</a>
   */
  private static final Set<String> RESERVED_WORDS = ImmutableSet.of("abs", "all", "and", "andthen", "any",
          "array", "as", "asc", "avg", "bag", "boolean", "by", "byte", "char", "collection",
          "count", "date", "declare", "define", "desc", "dictionary", "distinct", "double",
          "element", "enum", "except", "exists", "false", "first", "flatten", "float", "for", "from",
          "group", "having", "import", "in", "int", "intersect", "interval", "is_defined", "is_undefined",
          "last", "like", "limit", "list", "listtoset", "long", "map", "max", "min", "mod", "nil", "not",
          "null", "nvl", "octet", "or", "order", "orelse", "query", "select", "set", "short", "some",
          "string", "struct", "sum", "time", "timestamp", "to_date", "true", "type", "undefine", "undefined",
          "union", "unique", "where");

  private final PathNaming delegate;

  private ReservedWordNaming(PathNaming delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public String name(Path path) {
    String name = delegate.name(path);
    return Arrays.stream(name.split("\\."))
            .map(p -> RESERVED_WORDS.contains(p) ? '"' + p + '"' : p) // reserved word
            .collect(Collectors.joining("."));
  }

  static PathNaming of(PathNaming delegate) {
    return new ReservedWordNaming(delegate);
  }
}
