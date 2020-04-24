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

package org.immutables.criteria.backend;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.typemodel.StringHolderCriteria;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.immutables.check.Checkers.check;

/**
 * Test for "ID only expression" extractor
 */
class KeyLookupAnalyzerTest {

  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;

  /**
   * ID should only be extracted for {@code in} and {@code is} operators (not for others)
   */
  @Test
  void positive() {
    // only "is" and "in" call should pass
    check(ids(string.id.is(""))).isOf("");
    check(ids(string.id.is("a"))).isOf("a");
    check(ids(string.id.in("a", "b"))).isOf("a", "b");
    check(ids(string.id.in("b", "a"))).isOf("b", "a");
    check(ids(string.id.in(Collections.emptyList()))).isEmpty();
    check(ids(string.id.in(Collections.emptySet()))).isEmpty();
  }

  @Test
  void negative() {
    // negative cases
    check(ids(string.id.isNot("a"))).isEmpty();
    check(ids(string.id.notIn("a", "b"))).isEmpty();
    check(ids(string.id.notIn(Collections.emptyList()))).isEmpty();
    check(ids(string.id.greaterThan("a"))).isEmpty();
    check(ids(string.id.lessThan("a"))).isEmpty();
    check(ids(string.id.between("a", "b"))).isEmpty();

    // not a simple single expression
    check(ids(string.id.is("a").value.is("b"))).isEmpty();
    check(ids(string.id.is("a").or().id.is("b"))).isEmpty();
    check(ids(string.id.is("a").id.is("b"))).isEmpty();
    check(ids(string.id.is("a").id.is("a"))).isEmpty();

    // not ID attribute
    check(ids(string.value.is("a"))).isEmpty();
    check(ids(string.value.in("a", "b"))).isEmpty();
    check(ids(string.value.isNot("a"))).isEmpty();

    check(ids(string.nullable.is("a"))).isEmpty();
    check(ids(string.nullable.in("a", "b"))).isEmpty();
    check(ids(string.nullable.isNot("a"))).isEmpty();
  }

  @Test
  void disabled() {
    KeyLookupAnalyzer analyzer = KeyLookupAnalyzer.disabled();
    Expression expr = Criterias.toQuery(string.id.is("foo")).filter().get();

    KeyLookupAnalyzer.Result result = analyzer.analyze(expr);
    check(!result.isOptimizable());

    Assertions.assertThrows(UnsupportedOperationException.class, result::values);
  }

  private static List<String> ids(StringHolderCriteria crit) {
    Expression filter =Criterias.toQuery(crit).filter()
            .orElseThrow(() -> new AssertionError("No filter present"));

    KeyExtractor extractor = KeyExtractor.defaultFactory().create(TypeHolder.StringHolder.class);
    KeyLookupAnalyzer analyzer = KeyLookupAnalyzer.fromExtractor(extractor);
    KeyLookupAnalyzer.Result result = analyzer.analyze(filter);

    if (!result.isOptimizable()) {
      return Collections.emptyList();
    }

    return (List<String>) result.values();
  }

}
