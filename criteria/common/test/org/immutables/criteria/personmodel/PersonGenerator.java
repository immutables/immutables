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
import java.util.NoSuchElementException;
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

  private final static ImmutableList<String> CITIES = ImmutableList.of("Washington", "New York", "Springfield", "Greenville", "Madison", "Franklin");
  private final static ImmutableList<String> STREETS = ImmutableList.of("1201 Elm", "10 Lake", "22 Pine", "33 Hill");

  
  private final AtomicLong index;
  private final Clock clock;
  private final int size;

  public PersonGenerator() {
    this(Clock.systemDefaultZone());
  }

  public PersonGenerator(Clock clock) {
    Preconditions.checkState(FIRST_NAMES.size() == LAST_NAMES.size(), "%s != %s",
            FIRST_NAMES.size(), LAST_NAMES.size());
    this.index = new AtomicLong();
    this.clock = clock;
    this.size = FIRST_NAMES.size();
  }

  /**
   * Generates next valid instance of {@link Person}
   *
   * @return new {@link Person} instance
   */
  public ImmutablePerson next() {
    return get((int) index.getAndIncrement() % size);
  }

  /**
   * Generate person at particular "index"
   */
  private ImmutablePerson get(int index) {
    Preconditions.checkArgument(index < size,
            "expected index %s < %s", index, size);

    // add variation to ages so it doesn't look too consecutive 20, 21, 22 etc.
    final int age = 20 + (index * 37 >> 4) % 30;

    final ImmutableAddress address = ImmutableAddress.builder()
            .street(STREETS.get(index % STREETS.size()))
            .city(CITIES.get(index % CITIES.size()))
            .state(Address.State.values()[index % Address.State.values().length])
            .zip(age + "000")
            .build();

    final ImmutablePerson.Builder builder = ImmutablePerson.builder()
                .fullName(FIRST_NAMES.get(index) + " " + LAST_NAMES.get(index))
                .id("id" + index)
                .dateOfBirth(LocalDate.now(clock).minusYears(age))
                .isActive(index % 2 == 0)
                .age(age)
                .address(address)
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
   * Returns an iterator over generated entries. Number of elements depends on
   * internal constants like {@link #FIRST_NAMES}.
   *
   * @return iterator with valid entries
   */
  @Override
  public Iterator<ImmutablePerson> iterator() {
    return new Iterator<ImmutablePerson>() {
      private int index;
      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public ImmutablePerson next() {
        if (!hasNext()) {
          throw new NoSuchElementException("At index " + index + " while size is " + size);
        }
        return get(index++);
      }
    };
  }
}
