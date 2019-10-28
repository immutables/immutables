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

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class AggregateQueryBuilderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .registerModule(new GuavaModule())
          .registerModule(new Jdk8Module());

  private final Mapping mapping = Mapping.ofElastic(PersonModel.MAPPING);

  @Test
  void agg1() {
    PersonCriteria person = PersonCriteria.person;

    // select nickName, sum(age), max(dateOfBirth) from person
    //   where age >= 30
    //   group by nickName
    //   order by nickName
    //   limit 11
    Query query = Query.of(Person.class)
            .addGroupBy(Matchers.toExpression(person.nickName))
            .addCollations(Collections.singleton(Collation.of(Matchers.toExpression(person.nickName))))
            .addProjections(Matchers.toExpression(person.nickName), Matchers.toExpression(person.age.sum()), Matchers.toExpression(person.dateOfBirth.max()))
            .withFilter(Criterias.toQuery(person.age.atLeast(30)).filter().get())
            .withLimit(11);

    AggregateQueryBuilder builder = new AggregateQueryBuilder(query, MAPPER, mapping);
    ObjectNode json = builder.jsonQuery();

    JsonChecker.of(json).is("{'_source':false,",
            "size:0,",
            "'stored_fields':'_none_',",
            "'query.constant_score.filter.range.age.gte' : 30,",
            "aggregations:{expr0:{terms:{field:'nickName',missing:'__MISSING__',size:11,"
                    + " order:{'_key':'asc'}},",
            "aggregations:{expr1:{sum:{field:'age'}},expr2:{max:{field:'dateOfBirth'}}}}}}");
  }

}
