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

import java.time.LocalDate;
import java.util.Objects;

public final class PersonGenerator {

  private PersonGenerator() {}

  /**
   * Creates a simple person directly from fullName
   */
  static Person of(String fullName) {
    Objects.requireNonNull(fullName, "fullName");
    return ImmutablePerson.builder()
            .fullName(fullName)
            .id(Integer.toHexString(fullName.hashCode()))
            .dateOfBirth(LocalDate.of(1997, 4, 15))
            .isActive(true)
            .age(22)
            .build();
  }

}
