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

import com.google.common.collect.ImmutableSet;
import io.reactivex.Flowable;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.ImmutableDeleteByKey;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

public abstract class DeleteByKeyTemplate {

  private final StringHolderRepository repository;
  private final Backend.Session session;
  private final Supplier<ImmutableStringHolder> generator;

  protected DeleteByKeyTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.session = backend.open(TypeHolder.StringHolder.class);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  void empty() {
    check(execute(ImmutableDeleteByKey.of(Collections.emptySet()))).isIn(WriteResult.unknown(), WriteResult.empty().withDeletedCount(0));
    check(execute(ImmutableDeleteByKey.of(Collections.singleton("aaa")))).isIn(WriteResult.unknown(), WriteResult.empty().withDeletedCount(0));
    check(execute(ImmutableDeleteByKey.of(ImmutableSet.of("a", "b")))).isIn(WriteResult.unknown(), WriteResult.empty().withDeletedCount(0));
  }

  @Test
  void single() {
    repository.insert(generator.get().withId("id1"));

    check(execute(ImmutableDeleteByKey.of(Collections.emptySet()))).isIn(WriteResult.unknown(), WriteResult.empty().withDeletedCount(0));
    check(repository.findAll().fetch()).hasSize(1);

    check(execute(ImmutableDeleteByKey.of(Collections.singleton("aaa")))).isIn(WriteResult.unknown(), WriteResult.empty().withDeletedCount(0));
    check(repository.findAll().fetch()).hasSize(1);

    check(execute(ImmutableDeleteByKey.of(Collections.singleton("id1")))).isIn(WriteResult.unknown(), WriteResult.empty().withDeletedCount(1));
    check(repository.findAll().fetch()).isEmpty();
  }

  @Test
  void multiple() {
    repository.insert(generator.get().withId("id1"));
    repository.insert(generator.get().withId("id2"));
    check(execute(ImmutableDeleteByKey.of(ImmutableSet.of("id2", "id1", "MISSING")))).isIn(WriteResult.unknown(), WriteResult.empty().withDeletedCount(2));
    check(repository.findAll().fetch()).isEmpty();
  }

  private WriteResult execute(StandardOperations.DeleteByKey op) {
    return Flowable.fromPublisher(session.execute(op).publisher())
            .singleOrError()
            .cast(WriteResult.class)
            .blockingGet();
  }

}
