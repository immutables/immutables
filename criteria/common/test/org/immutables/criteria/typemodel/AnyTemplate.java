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
import org.immutables.criteria.matcher.IterableMatcher;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

/**
 * Testing {@link IterableMatcher#any()} functionality of the matcher.
 * At least one element in sub-array matches.
 */
public abstract class AnyTemplate {

  private final StringHolderRepository repository;
  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;
  private final Supplier<ImmutableStringHolder> generator;

  protected AnyTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  void empty() {
    ids(string.array.any().is("")).isEmpty();
    ids(string.array.any().isNot("")).isEmpty();
    ids(string.array.any().is("a")).isEmpty();
    ids(string.array.any().isNot("a")).isEmpty();
    ids(string.array.any().isEmpty()).isEmpty();
    ids(string.array.any().notEmpty()).isEmpty();
  }

  private IterableChecker<List<String>, String> ids(StringHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.StringHolder>ofReader(repository.find(criteria)).toList(TypeHolder.StringHolder::id);
  }

}
