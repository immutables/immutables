/*
   Copyright 2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.immutables.mongo.fixture.criteria;

import org.bson.BsonDocument;
import org.immutables.mongo.fixture.MongoAsserts;
import org.immutables.mongo.fixture.MongoContext;
import org.immutables.mongo.repository.Repositories;
import org.junit.Rule;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

/**
 * Comparing queries generated directly by criteria.
 */
public class QueryTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  private final PersonRepository repository = new PersonRepository(context.setup());

  @Test
  public void query() {
    final PersonRepository.Criteria criteria = repository.criteria();


    repository.find(criteria)
            .orderByAliases()
            .orderByNameDesceding();

    validate(criteria.name("foo"), BsonDocument.parse("{\"name\":\"foo\"}"));
    validate(criteria.name("foo").age(8), BsonDocument.parse("{\"name\":\"foo\", \"age\": 8}"));
    validate(criteria.name("foo").ageLessThan(8), BsonDocument.parse("{\"name\":\"foo\", \"age\": {\"$lt\": 8}}"));
    validate(criteria.ageLessThan(8).ageNot(5),
            BsonDocument.parse("{\"$and\": [{\"age\":{ \"$lt\": 8}}, {\"age\": {\"$ne\": 5}} ] }"));
    // pass as-is without optimizations
    validate(criteria.name("foo").name("bar"),
            BsonDocument.parse("{\"$and\": [{\"name\": \"foo\"}, {\"name\": \"bar\"}]}"));
    validate(criteria.name("foo").name("bar").name("qux"),
            BsonDocument.parse("{\"$and\": [{\"name\": \"foo\"}, {\"name\": \"bar\"}, {\"name\":\"qux\"}]}"));
    validate(criteria.name("foo").or().nameNot("bar"),
            BsonDocument.parse("{\"$or\": [{\"name\": \"foo\"}, {\"name\": {\"$ne\": \"bar\"}} ] }"));
    validate(criteria.nameIn("foo", "bar").name("bar"),
            BsonDocument.parse("{\"$and\":[ {\"name\": {\"$in\": [\"foo\", \"bar\"]}}, {\"name\": \"bar\"} ] }"));

    validate(criteria.name("foo").age(22).or().name("bar").age(33),
            BsonDocument.parse("{\"$or\": [" +
                    "{\"name\": \"foo\", \"age\": 22}, " +
                    "{\"name\": \"bar\", \"age\": 33}" +
                    "]}"));
  }

  /**
   * Converts given {@code criteria} to bson and compares with {@code expected}.
   *
   * @param criteria existing criteria to be sent to mogno (usually mongo query)
   * @param expected expected query (as bson)
   */
  private static void validate(Repositories.Criteria criteria, BsonDocument expected) {
    BsonDocument actual = MongoAsserts.extractQuery(criteria);
    check(actual).is(expected);
  }
}
