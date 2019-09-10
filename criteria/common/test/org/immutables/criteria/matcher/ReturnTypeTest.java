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

import com.google.common.reflect.TypeToken;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.immutables.check.Checkers.check;

public class ReturnTypeTest {

  private final PersonCriteria person = PersonCriteria.person;

  @Test
  public void localDate() {
    check(Matchers.toExpression(person.dateOfBirth.min()).returnType()).is(LocalDate.class);
    check(Matchers.toExpression(person.dateOfBirth.max()).returnType()).is(LocalDate.class);
    check(Matchers.toExpression(person.dateOfBirth.count()).returnType()).is(Long.class);
    check(Matchers.toExpression(person.dateOfBirth).returnType()).is(LocalDate.class);
  }

  @Test
  public void integer() {
    check(Matchers.toExpression(person.age.min()).returnType()).is(Integer.class);
    check(Matchers.toExpression(person.age.max()).returnType()).is(Integer.class);
    check(Matchers.toExpression(person.age.avg()).returnType()).is(Double.class);
    check(Matchers.toExpression(person.age.sum()).returnType()).is(Long.class);
    check(Matchers.toExpression(person.age.count()).returnType()).is(Long.class);
    check(Matchers.toExpression(person.age).returnType()).is(int.class);
  }

  @Test
  public void booleanType() {
    check(Matchers.toExpression(person.isActive).returnType()).is(boolean.class);
    check(Matchers.toExpression(person.isActive.count()).returnType()).is(Long.class);
  }

  @Test
  public void string() {
    check(Matchers.toExpression(person.fullName).returnType()).is(String.class);
    check(Matchers.toExpression(person.fullName.min()).returnType()).is(String.class);
    check(Matchers.toExpression(person.fullName.max()).returnType()).is(String.class);
    check(Matchers.toExpression(person.fullName.count()).returnType()).is(Long.class);
    check(Matchers.toExpression(person.nickName).returnType()).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.toExpression(person.nickName.min()).returnType()).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.toExpression(person.nickName.max()).returnType()).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.toExpression(person.nickName.count()).returnType()).is(Long.class);
  }

  @Test
  public void debug() {
    check(Matchers.toExpression(person.dateOfBirth.min()).returnType()).is(LocalDate.class);
  }


}
