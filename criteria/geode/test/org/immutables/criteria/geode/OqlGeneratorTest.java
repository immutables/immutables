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
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.typemodel.TypeHolder;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.immutables.check.Checkers.check;

/**
 * Check generated OQL out of {@link Query}
 */
class OqlGeneratorTest {

  private final ReservedWordsCriteria reservedWords = ReservedWordsCriteria.reservedWords;

  @Test
  void basic() {
    OqlGenerator generator = OqlGenerator.of("/myRegion", PathNaming.defaultNaming());
    check(generator.generate(Query.of(TypeHolder.StringHolder.class)).oql()).is("SELECT * FROM /myRegion");
    check(generator.generate(Query.of(TypeHolder.StringHolder.class).withLimit(1)).oql()).is("SELECT * FROM /myRegion LIMIT 1");

    Expression proj1 = Matchers.toExpression(reservedWords.value);
    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(proj1))
            .oql()).is("SELECT value FROM /myRegion");

    Expression proj2 = Matchers.toExpression(reservedWords.nullable);
    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(proj1, proj2))
            .oql()).is("SELECT value, nullable FROM /myRegion");
  }

  @Test
  void countAll() {
    OqlGenerator generator = OqlGenerator.of("/myRegion", PathNaming.defaultNaming());
    check(generator.generate(Query.of(TypeHolder.StringHolder.class).withCount(true)).oql()).is("SELECT COUNT(*) FROM /myRegion");
  }

  @Test
  void pathNaming() {
    OqlGenerator generator = OqlGenerator.of("/myRegion", path -> path.toStringPath() + "1");
    Expression type = Matchers.toExpression(reservedWords.type);
    Expression value = Matchers.toExpression(reservedWords.value);
    Expression select = Matchers.toExpression(reservedWords.select);
    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(type))
            .oql()).is("SELECT type1 FROM /myRegion");

    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(type, value))
            .oql()).is("SELECT type1, value1 FROM /myRegion");

    check(generator.withoutBindVariables().generate(Query.of(TypeHolder.StringHolder.class)
            .addProjections(type, value)
            .withFilter(Expressions.call(Operators.EQUAL, select, Expressions.constant(42))))
            .oql()).is("SELECT type1, value1 FROM /myRegion WHERE select1 = 42");

    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(type, value).addGroupBy(type))
            .oql()).is("SELECT type1, value1 FROM /myRegion GROUP BY type1");

    check(generator.generate(Query.of(TypeHolder.StringHolder.class)
            .addGroupBy(type)
            .addCollations(Collections.singleton(Collation.of(type)))
            .addProjections(type, value))
            .oql()).is("SELECT type1, value1 FROM /myRegion GROUP BY type1 ORDER BY type1");

  }

  /**
   *  <a href="https://geode.apache.org/docs/guide/19/developing/querying_basics/reserved_words.html">reserved</a> words in Geode
   *  have to be enclosed in double quotation marks like {@code "type"} or {@code "where"}
   */
  @Test
  void reservedWords() {
    OqlGenerator generator = OqlGenerator.of("/myRegion", ReservedWordNaming.of(PathNaming.defaultNaming()));

    Expression type = Matchers.toExpression(reservedWords.type);
    Expression order = Matchers.toExpression(reservedWords.order);
    Expression date = Matchers.toExpression(reservedWords.date);
    Expression select = Matchers.toExpression(reservedWords.select);
    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(type))
            .oql()).is("SELECT \"type\" FROM /myRegion");

    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(type, order))
            .oql()).is("SELECT \"type\", \"order\" FROM /myRegion");

    check(generator.generate(Query.of(TypeHolder.StringHolder.class).addProjections(type, order, date))
            .oql()).is("SELECT \"type\", \"order\", \"date\" FROM /myRegion");

    check(generator.generate(Query.of(TypeHolder.StringHolder.class)
            .addGroupBy(type)
            .addCollations(Collections.singleton(Collation.of(type)))
            .addProjections(type, order, date, select))
            .oql()).is("SELECT \"type\", \"order\", \"date\", \"select\" FROM /myRegion GROUP BY \"type\" ORDER BY \"type\"");
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