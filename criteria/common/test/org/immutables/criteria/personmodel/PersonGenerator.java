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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Generates {@link Person} objects with some pre-populated data. To be used for tests
 * to speedup entity creation (avoid providing all required fields).
 */
public final class PersonGenerator implements Iterable<ImmutablePerson> {

  private final static ImmutableList<String> FIRST_NAMES = ImmutableList.of("John", "James",
          "Robert", "Jennifer", "Michael", "Linda", "William", "Elizabeth", "Lucas", "Lea");

  private final static ImmutableList<String> LAST_NAMES = ImmutableList.of("Smith", "Green",
          "Brown", "Lopez", "Rodriguez", "Miller", "Cox", "Taylor", "Petit", "Leroy");

  private final static ImmutableList<String> NICKNAMES = ImmutableList.of("007", "bitmap",
          "flake", "superman", "blade", "clea", "calypso", "slyde", "hulk");
  
  private final AtomicLong index;
  private final Clock clock;

  public PersonGenerator() {
    this(Clock.systemDefaultZone());
  }

  public PersonGenerator(Clock clock) {
    Preconditions.checkState(FIRST_NAMES.size() == LAST_NAMES.size(), "%s != %s",
            FIRST_NAMES.size(), LAST_NAMES.size());
    this.index = new AtomicLong();
    this.clock = clock;
  }

  public ImmutablePerson next() {
    return get((int) index.getAndIncrement() % FIRST_NAMES.size());
  }

  /**
   * Generate person at particular "index"
   */
  private ImmutablePerson get(int index) {
    Preconditions.checkArgument(index < FIRST_NAMES.size(),
            "expected index %s < %s", index, FIRST_NAMES.size());

    // add variation to ages so it doesn't look too consecutive 20, 21, 22 etc.
    final int age = 20 + (index * 37 >> 4) % 30;

    final ImmutablePerson.Builder builder = ImmutablePerson.builder()
                .fullName(FIRST_NAMES.get(index) + " " + LAST_NAMES.get(index))
                .id("id" + index)
                .dateOfBirth(LocalDate.now(clock).minusYears(age))
                .isActive(index % 2 == 0)
                .age(age)
                .nickName(NICKNAMES.get(index % NICKNAMES.size()));

    return builder.build();
  }

  /**
   * Unbounded stream over generated entries
   */
  public Stream<ImmutablePerson> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /**
   * Returns an infinite iterator over generated entries. You're advised to use a limiting function
   * like {@code Iterators.limit(...)}.
   *
   * @return unbounded iterator with valid entries
   */
  @Override
  public Iterator<ImmutablePerson> iterator() {
    return new Iterator<ImmutablePerson>() {
      private int index;
      @Override
      public boolean hasNext() {
        return index < FIRST_NAMES.size();
      }

      @Override
      public ImmutablePerson next() {
        return get(index++);
      }
    };
  }
}
