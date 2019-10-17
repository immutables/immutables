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

import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.bson.json.JsonWriterSettings;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    AggregationQuery agg = new AggregationQuery(query, new MongoPathNaming());
    List<BsonDocument> actual = agg.toPipeline().stream()
            .map(b -> b.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry()))
            .collect(Collectors.toList());
    List<BsonDocument> expected = Arrays.stream(lines).map(BsonDocument::parse).collect(Collectors.toList());
    if (!actual.equals(expected)) {
      final JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
      // outputs Bson in pretty Json format (with new lines)
      // so output is human friendly in IDE diff tool
      final Function<List<BsonDocument>, String> prettyFn = bsons -> bsons.stream()
              .map(b -> b.toJson(settings)).collect(Collectors.joining("\n"));

      // used to pretty print Assertion error
      Assertions.assertEquals(
              prettyFn.apply(expected),
              prettyFn.apply(actual),
              "expected and actual Mongo pipelines do not match");

      Assertions.fail("Should have failed previously because expected != actual is known to be true");
    }
  }
}