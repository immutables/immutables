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

package org.immutables.criteria.inmemory;

import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.immutables.check.Checkers.check;

class InMemoryTest {

  @Test
  void predicate() {
    Predicate<Person> predicate = InMemory.toPredicate(PersonCriteria.person.age.is(22));
    PersonGenerator generator = new PersonGenerator();
    check(predicate.test(generator.next().withAge(22)));
    check(!predicate.test(generator.next().withAge(23)));
  }
}