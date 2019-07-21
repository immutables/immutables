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

package org.immutables.criteria.personmodel;

import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;

public class PersonTest {

  @Test
  @Ignore("used for compile-time testing only")
  public void collection() {
    PersonCriteria.person
            .pets.any().name.isNotEmpty()
            .age.isAtMost(22)
            .isActive.isFalse()
            .interests.none().startsWith("foo")
            .or()//.or() should not work
            .isActive.isTrue()
            .or()
            .fullName.not(f -> f.contains("bar").or().contains("foo"))
            .pets.all().name.isNotEmpty()
            .pets.any().name.with(n -> n.endsWith("aaa").or().startsWith("bbb"))
            .pets.any().name.contains("aaa")
            .pets.any().name.not(n -> n.contains("bar"))
            .pets.none().name.hasLength(3)
            .not(p -> p.pets.hasSize(2))
            .dateOfBirth.isAtMost(LocalDate.MAX)
            .interests.contains("test");
  }

}