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

package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.immutables.check.Checkers.check;

class MatchersTest {

  @Test
  void projection() {
    Expression expr1 = Matchers.toExpression(PersonCriteria.person.fullName);
    check(expr1 instanceof Path);
    check(((Path) expr1).toStringPath()).is("fullName");

    Expression expr2 = Matchers.toExpression(PersonCriteria.person.bestFriend.value().hobby);
    check(expr2 instanceof Path);
    check(((Path) expr2).toStringPath()).is("bestFriend.hobby");
  }

  @Test
  void componentType() throws Exception {
    check(Matchers.iterableTypeArgument(Dummy.getField("stringList"))).is(String.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("stringSet"))).is(String.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("stringCollection"))).is(String.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("stringArrayList"))).is(String.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("stringArray"))).is(String.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("stringIterable"))).is(String.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("booleanArray"))).is(boolean.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("booleanIterable"))).is(Boolean.class);
    check(Matchers.iterableTypeArgument(Dummy.getField("intArray"))).is(int.class);
  }

  private static class Dummy {
    private List<String> stringList;
    private Set<String> stringSet;
    private Collection<String> stringCollection;
    private ArrayList<String> stringArrayList;
    private String[] stringArray;
    private Iterable<String> stringIterable;
    private boolean[] booleanArray;
    private Iterable<Boolean> booleanIterable;
    private int[] intArray;

    private static Type getField(String name) throws NoSuchFieldException {
      return Dummy.class.getDeclaredField(name).getGenericType();
    }
  }

}