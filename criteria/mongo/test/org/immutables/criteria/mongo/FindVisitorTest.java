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
import org.immutables.criteria.matcher.PresentAbsentMatcher;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.typemodel.StringHolderCriteria;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

class FindVisitorTest {

  private final StringHolderCriteria criteria = StringHolderCriteria.stringHolder;
  private final PersonCriteria person = PersonCriteria.person;

  /**
   * Test queries for ID field ({@code _id})
   */
  @Test
  void id() {
    check(this.criteria.id.is("id1")).matches("{_id: 'id1'}");
    check(this.criteria.id.in(Arrays.asList("id1", "id2"))) .matches("{_id: {$in: ['id1', 'id2']}}");
    check(this.criteria.id.isNot("id1")) .matches("{_id: {$ne: 'id1'}}");
    check(this.criteria.id.notIn(Arrays.asList("id1", "id2"))) .matches("{_id: {$nin: ['id1', 'id2']}}");
  }

  /**
   * Test a non-id ({@code _id}) field
   */
  @Test
  void value_notId() {
    check(this.criteria.value.is("v1")).matches("{value: 'v1'}");
    check(this.criteria.value.in(Arrays.asList("v1", "v2"))) .matches("{value: {$in: ['v1', 'v2']}}");
    check(this.criteria.value.isNot("v1")) .matches("{value: {$ne: 'v1'}}");
    check(this.criteria.value.notIn(Arrays.asList("v1", "v2"))) .matches("{value: {$nin: ['v1', 'v2']}}");
  }

  /**
   * {@code in} / {@code notIn} with single element converted to {@code eq} / {@code ne}
   */
  @Test
  void singleElementIn() {
    check(this.criteria.value.in(Collections.singleton("v1"))).matches("{value: 'v1'}");
    check(this.criteria.value.notIn(Collections.singleton("v1"))).matches("{value: {$ne: 'v1'}}");
  }

  @Test
  void absentPresent() {
    String present = "{$and: [{optional: {$exists: true}}, {optional: {$ne: null}}]}";
    String absent = "{$or: [{optional: {$exists: false}}, {optional: null}]}";
    check(this.criteria.optional.isPresent()).matches(present);
    check(this.criteria.optional.isAbsent()).matches(absent);

    // negation of absent / present
    check(this.criteria.optional.not(PresentAbsentMatcher::isAbsent)).matches(present);
    check(this.criteria.optional.not(PresentAbsentMatcher::isPresent)).matches(absent);
  }

  @Test
  void emptyString() {
    check(this.criteria.value.isEmpty()).matches("{value: ''}");
    check(this.criteria.optional.isEmpty()).matches("{optional: ''}");
    check(this.criteria.value.notEmpty()).matches("{$and: [{value: {$nin: ['', null]}}, {value: {$exists: true}}]}");
  }

  @Test
  void emptyIterable() {
    check(this.criteria.list.isEmpty()).matches("{list: []}");
    check(this.criteria.list.notEmpty()).matches("{$and: [{list: {$exists: true}}, {list:{$ne:null}}, {list: {$ne:[]}}]}");
  }

  @Test
  void upperLower() {
    check(this.criteria.value.toUpperCase().is("A")).matches("{$expr: {$eq:[{$toUpper: '$value'}, 'A']}}");
    check(this.criteria.value.toLowerCase().is("a")).matches("{$expr: {$eq:[{$toLower: '$value'}, 'a']}}");
    check(this.criteria.value.toLowerCase().isNot("a")).matches("{$expr: {$ne:[{$toLower: '$value'}, 'a']}}");
    check(this.criteria.value.toLowerCase().in("a", "b")).matches("{$expr: {$in:[{$toLower: '$value'}, ['a', 'b']]}}");
    // for aggregations / $expr, $nin does not work
    // use {$not: {$in: ... }} instead
    check(this.criteria.value.toUpperCase().notIn("a", "b")).matches("{$expr: {$not: {$in:[{$toUpper: '$value'}, ['a', 'b']]}}}");

    // chain toUpper.toLower.toUpper
    check(this.criteria.value.toUpperCase().toLowerCase().is("A"))
            .matches("{$expr: {$eq:[{$toLower: {$toUpper: '$value'}}, 'A']}}");

    check(this.criteria.value.toLowerCase().toUpperCase().is("A"))
            .matches("{$expr: {$eq:[{$toUpper: {$toLower: '$value'}}, 'A']}}");

  }

  /**
   * Scalar {@code List<String>} with {@code any()}: positive operators use flat dot-path,
   * negative operators must use {@code $elemMatch} for existential semantics.
   */
  @Test
  void iterableAnyScalar() {
    // positive operators: flat dot-path (existing behaviour)
    check(this.criteria.list.any().is("x")).matches("{list: 'x'}");
    check(this.criteria.list.any().in(Arrays.asList("x", "y"))).matches("{list: {$in: ['x','y']}}");
    // negative operators: $elemMatch required for existential ("at least one") semantics
    check(this.criteria.list.any().isNot("x")).matches("{list: {$elemMatch: {$ne: 'x'}}}");
    check(this.criteria.list.any().notIn(Arrays.asList("x", "y"))).matches("{list: {$elemMatch: {$nin: ['x','y']}}}");
  }

  /**
   * Nested-object {@code List<Pet>} with {@code any()}: positive operators use flat dot-path,
   * negative operators must use {@code $elemMatch}.
   */
  @Test
  void iterableAnyNestedObject() {
    // positive: flat dot-path
    check(this.person.pets.any().name.is("fluffy")).matches("{'pets.name': 'fluffy'}");
    // negative: $elemMatch
    check(this.person.pets.any().name.isNot("fluffy")).matches("{pets: {$elemMatch: {name: {$ne: 'fluffy'}}}}");
    check(this.person.pets.any().name.notIn(Arrays.asList("gecko", "oopsy"))).matches("{pets: {$elemMatch: {name: {$nin: ['gecko','oopsy']}}}}");
  }

  /**
   * Compound conditions inside {@code any().with()} / {@code any().not()} produce
   * {@code $elemMatch} with {@code $and} / {@code $or} / negated operators.
   */
  @Test
  void iterableAnyCompound() {
    // AND inside any() -> $elemMatch with $and
    check(this.person.pets.any().with(p -> p.name.is("fluffy").and(p.name.is("buddy"))))
        .matches("{pets: {$elemMatch: {$and: [{name: 'fluffy'}, {name: 'buddy'}]}}}");
    // OR inside any() -> $elemMatch with $or
    check(this.person.pets.any().with(p -> p.name.is("fluffy").or(p.name.is("buddy"))))
        .matches("{pets: {$elemMatch: {$or: [{name: 'fluffy'}, {name: 'buddy'}]}}}");
    // NOT inside any() -> $elemMatch with negated operator
    check(this.person.pets.any().not(p -> p.name.is("fluffy")))
        .matches("{pets: {$elemMatch: {name: {$ne: 'fluffy'}}}}");
  }

  private static QueryAssertion check(StringHolderCriteria criteria) {
    return QueryAssertion.ofFilter(Criterias.toQuery(criteria));
  }

  private static QueryAssertion check(PersonCriteria criteria) {
    return QueryAssertion.ofFilter(Criterias.toQuery(criteria));
  }
}