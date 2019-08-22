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

package org.immutables.criteria;

import org.immutables.criteria.expression.DebugExpressionVisitor;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.matcher.Aggregation;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AggregationExpressionTest {

  @Test
  public void aggregation() {
    PersonCriteria person = PersonCriteria.person;
    assertProjection(person.age.count(), "call op=COUNT path=age");
    assertProjection(person.age.sum(), "call op=SUM path=age");
    assertProjection(person.age.avg(), "call op=AVG path=age");
    assertProjection(person.age.min(), "call op=MIN path=age");
    assertProjection(person.age.max(), "call op=MAX path=age");
    assertProjection(person.bestFriend.value().hobby.max(), "call op=MAX path=bestFriend.hobby");
    assertProjection(person.bestFriend.value().hobby.count(), "call op=COUNT path=bestFriend.hobby");
    assertProjection(person.nickName.max(), "call op=MAX path=nickName");
  }

  private static void assertProjection(Aggregation<?> aggregation, String ... expectedLines) {
    Expression expression = Matchers.toExpression(aggregation);
    StringWriter out = new StringWriter();
    expression.accept(new DebugExpressionVisitor<>(new PrintWriter(out)));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assert.assertEquals(expected, out.toString().trim());
  }



}
