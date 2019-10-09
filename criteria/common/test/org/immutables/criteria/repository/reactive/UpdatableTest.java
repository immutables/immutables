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

package org.immutables.criteria.repository.reactive;

import io.reactivex.Single;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.personmodel.ImmutablePerson;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.repository.Updater;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class UpdatableTest {

  @Test
  @Disabled("compile-time only")
  void name() {
    PersonCriteria person = PersonCriteria.person;
    Updater<Person, Single<WriteResult>> updater = null;

    updater.set(person.nickName, Optional.of("aaa"))
            .set(person, ImmutablePerson.builder().build()) // instead of replace ?
            .set(person.fullName, "John")
            .set(person.isActive, true)
            .execute();

    updater.replace(ImmutablePerson.builder().build()).execute();
  }
}
