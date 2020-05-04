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

package org.immutables.criteria.typemodel;

import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.matcher.PresentAbsentMatcher;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Testing operation on composite / nested attributes {@code a.b.c}
 */
public abstract class CompositeTemplate {

  private final CompositeHolderRepository repository;
  private final Supplier<ImmutableCompositeHolder> generator;
  private final CompositeHolderCriteria composite = CompositeHolderCriteria.compositeHolder;

  protected CompositeTemplate(Backend backend) {
    this.repository = new CompositeHolderRepository(backend);
    this.generator = TypeHolder.CompositeHolder.generator();
  }

  /**
   * Querying optional fields
   */
  @Test
  protected void optional() {
    ImmutableBooleanHolder optionalBoolean = TypeHolder.BooleanHolder.generator().get().withOptional(true);
    repository.insert(generator.get().withId("id1").withOptionalBoolean(optionalBoolean));
    repository.insert(generator.get().withId("id2").withOptionalBoolean(Optional.empty()));

    ids(composite.optionalBoolean.isPresent()).isOf("id1");
    ids(composite.optionalBoolean.isAbsent()).isOf("id2");

    ids(composite.optionalBoolean.value().optional.isPresent()).isOf("id1");
    ids(composite.optionalBoolean.value().optional.isAbsent()).isOf("id2");

    ids(composite.optionalBoolean.value().optional.isTrue()).isOf("id1");
    ids(composite.optionalBoolean.value().optional.isFalse()).isEmpty();
  }

  /**
   * Querying nullable fields
   */
  @Test
  protected void nullable() {
    ImmutableBooleanHolder nullableBoolean = TypeHolder.BooleanHolder.generator().get().withNullable(true);
    repository.insert(generator.get().withId("id1").withNullableBoolean(nullableBoolean));
    repository.insert(generator.get().withId("id2").withNullableBoolean(null));

    ids(composite.nullableBoolean.isPresent()).isOf("id1");
    ids(composite.nullableBoolean.isAbsent()).isOf("id2");

    ids(composite.nullableBoolean.value().nullable.isPresent()).isOf("id1");
    ids(composite.nullableBoolean.value().nullable.isAbsent()).isOf("id2");

    // not
    ids(composite.nullableBoolean.value().nullable.not(PresentAbsentMatcher::isPresent)).isOf("id2");
    ids(composite.nullableBoolean.value().nullable.not(PresentAbsentMatcher::isAbsent)).isOf("id1");

    ids(composite.nullableBoolean.value().nullable.isTrue()).isOf("id1");
    ids(composite.nullableBoolean.value().nullable.isFalse()).isEmpty();
  }

  /**
   * Return {@link TypeHolder.StringHolder#id()} after applying a criteria
   */
  private IterableChecker<List<String>, String> ids(CompositeHolderCriteria criteria) {
    return  CriteriaChecker.<TypeHolder.CompositeHolder>ofReader(repository.find(criteria)).toList(TypeHolder.CompositeHolder::id);
  }

}
