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

package org.immutables.criteria.typemodel;

import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.CriteriaChecker;

import java.util.List;
import java.util.function.Supplier;

/**
 * Testing various string operations prefix/suffix/length etc.
 */
public abstract class LongTemplate {

  private final LongHolderRepository repository;
  private final LongHolderCriteria criteria = LongHolderCriteria.longHolder;
  private final Supplier<ImmutableLongHolder> generator;

  protected LongTemplate(Backend backend) {
    this.repository = new LongHolderRepository(backend);
    this.generator = TypeHolder.LongHolder.generator();
  }


  private IterableChecker<List<String>, String> ids(LongHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.LongHolder>of(repository.find(criteria)).toList(TypeHolder.LongHolder::id);
  }
}
