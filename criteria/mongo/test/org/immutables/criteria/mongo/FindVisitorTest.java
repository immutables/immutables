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

package org.immutables.criteria.mongo;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.typemodel.StringHolderCriteria;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

class FindVisitorTest {

  private final StringHolderCriteria criteria = StringHolderCriteria.stringHolder;

  /**
   * Test queries for ID field ({@code _id})
   */
  @Test
  void id() {
    check(criteria.id.is("id1")).matches("{_id: 'id1'}");
    check(criteria.id.in(Arrays.asList("id1", "id2"))) .matches("{_id: {$in: ['id1', 'id2']}}");
    check(criteria.id.isNot("id1")) .matches("{_id: {$ne: 'id1'}}");
    check(criteria.id.notIn(Arrays.asList("id1", "id2"))) .matches("{_id: {$nin: ['id1', 'id2']}}");
  }

  /**
   * Test a non-id ({@code _id}) field
   */
  @Test
  void value_notId() {
    check(criteria.value.is("v1")).matches("{value: 'v1'}");
    check(criteria.value.in(Arrays.asList("v1", "v2"))) .matches("{value: {$in: ['v1', 'v2']}}");
    check(criteria.value.isNot("v1")) .matches("{value: {$ne: 'v1'}}");
    check(criteria.value.notIn(Arrays.asList("v1", "v2"))) .matches("{value: {$nin: ['v1', 'v2']}}");
  }

  /**
   * {@code in} / {@code notIn} with single element converted to {@code eq} / {@code ne}
   */
  @Test
  void singleElementIn() {
    check(criteria.value.in(Collections.singleton("v1"))).matches("{value: 'v1'}");
    check(criteria.value.notIn(Collections.singleton("v1"))).matches("{value: {$ne: 'v1'}}");
  }

  @Test
  void absentPresent() {
    check(criteria.optional.isPresent()).matches("{optional: {$exists: true, $ne: null}}");
    check(criteria.optional.isAbsent()).matches("{optional: {$not: {$exists: true, $ne: null}}}");
  }

  @Test
  void emptyString() {
    check(criteria.value.isEmpty()).matches("{value: ''}");
    check(criteria.optional.isEmpty()).matches("{optional: ''}");
    check(criteria.value.notEmpty()).matches("{value: {$nin: ['', null], $exists: true}}");
  }

  @Test
  void upperLower() {
    check(criteria.value.toUpperCase().is("A")).matches("{$expr: {$eq:[{$toUpper: '$value'}, 'A']}}");
    check(criteria.value.toLowerCase().is("a")).matches("{$expr: {$eq:[{$toLower: '$value'}, 'a']}}");
    check(criteria.value.toLowerCase().isNot("a")).matches("{$expr: {$ne:[{$toLower: '$value'}, 'a']}}");
    check(criteria.value.toLowerCase().in("a", "b")).matches("{$expr: {$in:[{$toLower: '$value'}, ['a', 'b']]}}");
    // for aggregations / $expr,  $nin does not work
    // use {$not: {$in: ... }} instead
    check(criteria.value.toUpperCase().notIn("a", "b")).matches("{$expr: {$not: {$in:[{$toUpper: '$value'}, ['a', 'b']]}}}");

    // chain toUpper.toLower.toUpper
    check(criteria.value.toUpperCase().toLowerCase().is("A"))
            .matches("{$expr: {$eq:[{$toLower: {$toUpper: '$value'}}, 'A']}}");

    check(criteria.value.toLowerCase().toUpperCase().is("A"))
            .matches("{$expr: {$eq:[{$toUpper: {$toLower: '$value'}}, 'A']}}");

  }

  private static QueryAssertion check(StringHolderCriteria criteria) {
    return QueryAssertion.ofFilter(Criterias.toQuery(criteria));
  }
}