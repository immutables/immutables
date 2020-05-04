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

package org.immutables.criteria.mongo;

import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class AggregationQueryTest {

  private final PersonCriteria person = PersonCriteria.person;

  @Test
  void basic() {
    // select nickName, sum(age) from ... group by nickName
    Query query = Query.of(Person.class)
            .addGroupBy(Matchers.toExpression(person.nickName))
            .addProjections(Matchers.toExpression(person.nickName), Matchers.toExpression(person.age.sum()));

    assertAgg(query, "{$project: {expr0: '$nickName', expr1: '$age'}}",
            "{$group: {_id: '$expr0', 'expr1': {$sum: '$expr1'}}}",
            "{$project: {expr0: '$_id', expr1: '$expr1'}}");
  }

  @Test
  void withSort() {
    Query query = Query.of(Person.class)
            .addGroupBy(Matchers.toExpression(person.nickName))
            .addCollations(Collections.singleton(Collation.of(Matchers.toExpression(person.nickName))))
            .addProjections(Matchers.toExpression(person.nickName), Matchers.toExpression(person.age.sum()))
            .withOffset(1)
            .withLimit(2);

    assertAgg(query, "{$project: {expr0: '$nickName', expr1: '$age'}}",
            "{$group: {_id: '$expr0', 'expr1': {$sum: '$expr1'}}}",
            "{$project: {expr0: '$_id', expr1: '$expr1'}}",
            "{$sort: {expr0: 1}}",
            "{$skip: 1}",
            "{$limit: 2}");
  }

  /**
   * Testing single field: {@code select distinct nickName from ...}
   */
  @Test
  void distinct1() {
    ImmutableQuery query = Query.of(Person.class)
            .addProjections(Matchers.toExpression(person.nickName))
            .withDistinct(true);

    assertAgg(query, "{$project: {expr0: '$nickName'}}",
            "{$group: {_id: '$expr0'}}",
            "{$project: {expr0: '$_id'}}");

    assertAgg(query.withLimit(1), "{$project: {expr0: '$nickName'}}",
            "{$group: {_id: '$expr0'}}",
            "{$project: {expr0: '$_id'}}",
            "{$limit: 1}"
            );

    assertAgg(query.withLimit(1).withCount(true), "{$project: {expr0: '$nickName'}}",
            "{$group: {_id: '$expr0'}}",
            "{$project: {expr0: '$_id'}}",
            "{$limit: 1}",
            "{$count: 'count'}"
            );
  }

  /**
   * Testing two field (s): {@code select distinct a, b from ...}
   */
  @Test
  void distinct2() {
    ImmutableQuery query = Query.of(Person.class)
            .addProjections(Matchers.toExpression(person.nickName))
            .addProjections(Matchers.toExpression(person.age))
            .withDistinct(true);

    assertAgg(query, "{$project: {expr0: '$nickName', expr1: '$age'}}",
            "{$group: {_id: {expr0: '$expr0', expr1: '$expr1'}}}",
            "{$project: {expr0: '$_id.expr0', expr1: '$_id.expr1'}}");

    assertAgg(query.withLimit(1), "{$project: {expr0: '$nickName', expr1: '$age'}}",
            "{$group: {_id: {expr0: '$expr0', expr1: '$expr1'}}}",
            "{$project: {expr0: '$_id.expr0', expr1: '$_id.expr1'}}",
            "{$limit: 1}");

    assertAgg(query.withLimit(1).withCount(true), "{$project: {expr0: '$nickName', expr1: '$age'}}",
            "{$group: {_id: {expr0: '$expr0', expr1: '$expr1'}}}",
            "{$project: {expr0: '$_id.expr0', expr1: '$_id.expr1'}}",
            "{$limit: 1}",
            "{$count: 'count'}"
    );
  }

  @Disabled
  @Test
  void withCount() {

    Query query = Query.of(Person.class)
            .addGroupBy(Matchers.toExpression(person.nickName))
            .addProjections(Matchers.toExpression(person.nickName))
            .withCount(true);

    assertAgg(query, "{$project: {expr0: '$nickName'}}",
            "{$group: {_id: '$expr0'}}",
            "{$count: 'count'}");
  }

  private static void assertAgg(Query query, String ... lines) {
    QueryAssertion.ofPipeline(query).matchesMulti(lines);
  }
}