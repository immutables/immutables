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

package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Visitors;
import org.immutables.criteria.typemodel.BooleanHolderCriteria;
import org.immutables.criteria.typemodel.CompositeHolderCriteria;
import org.immutables.criteria.typemodel.IntegerHolderCriteria;
import org.immutables.criteria.typemodel.LocalDateHolderCriteria;
import org.immutables.criteria.typemodel.StringHolderCriteria;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.time.LocalDate;

import static org.immutables.check.Checkers.check;

class ComponentTypeTest {

  private static Type argumentOf(Projection<?> projection) {
    return Matchers.iterableTypeArgument(Visitors.toPath(Matchers.toExpression(projection)).returnType());
  }

  @Test
  void localDate() {
    final LocalDateHolderCriteria criteria = LocalDateHolderCriteria.localDateHolder;
    check(argumentOf(criteria.array)).is(LocalDate.class);
    check(argumentOf(criteria.list)).is(LocalDate.class);
  }

  @Test
  void string() {
    StringHolderCriteria criteria = StringHolderCriteria.stringHolder;
    check(argumentOf(criteria.array)).is(String.class);
    check(argumentOf(criteria.list)).is(String.class);
  }

  @Test
  void integer() {
    IntegerHolderCriteria criteria = IntegerHolderCriteria.integerHolder;
    check(argumentOf(criteria.array)).is(int.class);
    check(argumentOf(criteria.list)).is(Integer.class);
  }

  @Test
  void booleanType() {
    BooleanHolderCriteria criteria = BooleanHolderCriteria.booleanHolder;
    check(argumentOf(criteria.array)).is(boolean.class);
    check(argumentOf(criteria.list)).is(Boolean.class);
  }

  @Test
  void composite() {
    CompositeHolderCriteria criteria = CompositeHolderCriteria.compositeHolder;
    check(argumentOf(criteria.integerList)).is(TypeHolder.IntegerHolder.class);
    check(argumentOf(criteria.stringList)).is(TypeHolder.StringHolder.class);
  }
}
