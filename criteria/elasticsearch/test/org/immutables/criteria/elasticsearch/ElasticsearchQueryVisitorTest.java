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

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Predicate;

class ElasticsearchQueryVisitorTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Predicate<Path> idPredicate = Elasticsearch.idPredicate(KeyExtractor.defaultFactory().create(Person.class).metadata());
  private final PathNaming pathNaming = PathNaming.defaultNaming();
  private final PersonCriteria person = PersonCriteria.person;

  @Test
  void ids() {
    filterToJson(person.id.is("id1")).is("{ids:{values:['id1']}}");
    filterToJson(person.id.in("id1", "id2")).is("{ids:{values:['id1', 'id2']}}");
    filterToJson(person.id.in(Collections.emptyList())).is("{ids:{values:[]}}");
    filterToJson(person.id.isNot("id1")).is("{bool: {must_not: {ids:{values:['id1']}}}}");
  }

  private JsonChecker filterToJson(PersonCriteria criteria) {
    Expression expr = Criterias.toQuery(criteria).filter().get();
    ObjectNode node = Elasticsearch.toBuilder(expr, pathNaming, idPredicate).toJson(objectMapper);
    return JsonChecker.of(node);
  }

}