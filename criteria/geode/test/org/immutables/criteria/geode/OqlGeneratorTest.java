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

import org.immutables.criteria.Criteria;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.*;
import org.immutables.criteria.personmodel.Person;
import org.immutables.value.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.matcher.Matchers.toExpression;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

/**
 * Check generated OQL out of {@link Query}
 */
class OqlGeneratorTest {

  private final ReservedWordsCriteria reservedWords = ReservedWordsCriteria.reservedWords;

  private OqlGenerator generator;

  @BeforeEach
  void setUp() {
    generator = OqlGenerator.of("/myRegion", PathNaming.defaultNaming());
  }

  @Test
  void basic() {
    final ImmutableQuery query = Query.of(Person.class);
    check(generate(query)).is("SELECT * FROM /myRegion");
    check(generate(query.withDistinct(true))).is("SELECT DISTINCT * FROM /myRegion");

    Expression proj1 = toExpression(reservedWords.value);
    check(generate(Query.of(Person.class).addProjections(proj1)))
            .is("SELECT value FROM /myRegion");

    Expression proj2 = toExpression(reservedWords.nullable);
    check(generate(Query.of(Person.class).addProjections(proj1, proj2)))
            .is("SELECT value, nullable FROM /myRegion");
  }

  @Test
  void filter() {
    check(generate(Query.of(Person.class).withFilter(toExpression(person.age.greaterThan(18)))))
            .is("SELECT * FROM /myRegion WHERE age > $1");
  }

  @Test
  void orderBy() {
    final ImmutableQuery query = Query.of(Person.class);
    check(generate(query.withCollations((Collation) person.fullName.asc())))
            .is("SELECT * FROM /myRegion ORDER BY fullName");

    check(generate(query.withCollations((Collation) person.age.desc(), (Collation) person.fullName.asc())))
            .is("SELECT * FROM /myRegion ORDER BY age DESC, fullName ASC");

    check(generate(query.withCollations((Collation) person.fullName.desc()).withLimit(1).withOffset(10)))
            .is("SELECT * FROM /myRegion ORDER BY fullName DESC LIMIT 1 OFFSET 10");
  }

  @Test
  void projections() {
    final ImmutableQuery query = Query.of(Person.class);
    check(generate(query.addProjections(toExpression(person.fullName))))
            .is("SELECT fullName FROM /myRegion");

    check(generate(query.addProjections(
            toExpression(person.fullName), toExpression(person.age)
    ))).is("SELECT fullName, age FROM /myRegion");
  }

  @Test
  void countAll() {
    check(generate(Query.of(Person.class).withCount(true)))
            .is("SELECT COUNT(*) FROM /myRegion");

    check(generate(Query.of(Person.class).withDistinct(true).withCount(true)))
            .is("SELECT DISTINCT COUNT(*) FROM /myRegion");
  }

  @Test
  void pathNaming() {
    OqlGenerator generator = OqlGenerator.of("/myRegion", path -> path.toStringPath() + "1");
    Expression type = toExpression(reservedWords.type);
    Expression value = toExpression(reservedWords.value);
    Expression select = toExpression(reservedWords.select);
    check(generator.generate(Query.of(Person.class).addProjections(type))
            .oql()).is("SELECT type1 FROM /myRegion");

    check(generator.generate(Query.of(Person.class).addProjections(type, value))
            .oql()).is("SELECT type1, value1 FROM /myRegion");

    check(generator.withoutBindVariables().generate(Query.of(Person.class)
            .addProjections(type, value)
            .withFilter(Expressions.binaryCall(Operators.EQUAL, select, Expressions.constant(42))))
            .oql()).is("SELECT type1, value1 FROM /myRegion WHERE select1 = 42");

    check(generator.generate(Query.of(Person.class).addProjections(type, value).addGroupBy(type))
            .oql()).is("SELECT type1, value1 FROM /myRegion GROUP BY type1");

    check(generator.generate(Query.of(Person.class)
            .addGroupBy(type)
            .addCollations(Collections.singleton(Collation.of(type)))
            .addProjections(type, value))
            .oql()).is("SELECT type1, value1 FROM /myRegion GROUP BY type1 ORDER BY type1");

  }

  @Test
  void upperLower() {
    check(generate(person.fullName.toLowerCase().is("a")))
            .is("SELECT * FROM /myRegion WHERE fullName.toLowerCase() = $1");

    check(generate(Query.of(Person.class).withFilter(toExpression(person.fullName.toUpperCase().is("A")))))
            .is("SELECT * FROM /myRegion WHERE fullName.toUpperCase() = $1");

    check(generate(Query.of(Person.class).withFilter(toExpression(person.fullName.toUpperCase().toLowerCase().is("A")))))
            .is("SELECT * FROM /myRegion WHERE fullName.toUpperCase().toLowerCase() = $1");

    check(generate(Query.of(Person.class).withFilter(toExpression(person.fullName.toUpperCase().toLowerCase().toUpperCase().is("A")))))
            .is("SELECT * FROM /myRegion WHERE fullName.toUpperCase().toLowerCase().toUpperCase() = $1");

  }

  /**
   *  <a href="https://geode.apache.org/docs/guide/19/developing/querying_basics/reserved_words.html">reserved</a> words in Geode
   *  have to be enclosed in double quotation marks like {@code "type"} or {@code "where"}
   */
  @Test
  void reservedWords() {
    OqlGenerator generator = OqlGenerator.of("/myRegion", ReservedWordNaming.of(PathNaming.defaultNaming()));

    Expression type = toExpression(reservedWords.type);
    Expression order = toExpression(reservedWords.order);
    Expression date = toExpression(reservedWords.date);
    Expression select = toExpression(reservedWords.select);
    check(generator.generate(Query.of(Person.class).addProjections(type))
            .oql()).is("SELECT \"type\" FROM /myRegion");

    check(generator.generate(Query.of(Person.class).addProjections(type, order))
            .oql()).is("SELECT \"type\", \"order\" FROM /myRegion");

    check(generator.generate(Query.of(Person.class).addProjections(type, order, date))
            .oql()).is("SELECT \"type\", \"order\", \"date\" FROM /myRegion");

    check(generator.generate(Query.of(Person.class)
            .addGroupBy(type)
            .addCollations(Collections.singleton(Collation.of(type)))
            .addProjections(type, order, date, select))
            .oql()).is("SELECT \"type\", \"order\", \"date\", \"select\" FROM /myRegion GROUP BY \"type\" ORDER BY \"type\"");
  }


  private String generate(Criterion<?> person) {
    return generate(Criterias.toQuery(person));
  }

  private String generate(Query query) {
    return generator.generate(query).oql();
  }

  /**
   * Some of the reserved words in Geode.
   * For a full list see  <a href="https://geode.apache.org/docs/guide/19/developing/querying_basics/reserved_words.html">reserved words in Geode</a>
   */
  @Value.Immutable
  @Criteria
  interface ReservedWords {
    int type();
    int date();
    int select();
    int order();
    int value(); // not reserved
    int nullable(); // not reserved
  }

}