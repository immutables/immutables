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
import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.ImmutableGetByKey;
import org.immutables.criteria.backend.StandardOperations;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

public abstract class GetByKeyTemplate {

  private final StringHolderRepository repository;
  private final Backend.Session session;
  private final Supplier<ImmutableStringHolder> generator;

  protected GetByKeyTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.session = backend.open(TypeHolder.StringHolder.class);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  void empty() {
    ids(ImmutableGetByKey.of(Collections.emptySet())).isEmpty();
    ids(ImmutableGetByKey.of(Collections.singleton("s1"))).isEmpty();
    ids(ImmutableGetByKey.of(new HashSet<>(Arrays.asList("s1", "s2")))).isEmpty();
  }

  @Test
  void single() {
    repository.insert(generator.get().withId("i1"));
    ids(ImmutableGetByKey.of(Collections.emptySet())).isEmpty();
    ids(ImmutableGetByKey.of(Collections.singleton("MISSING"))).isEmpty();
    ids(ImmutableGetByKey.of(Collections.singleton("i1"))).isOf("i1");
    ids(ImmutableGetByKey.of(ImmutableSet.of("i1", "MISSING"))).isOf("i1");
    ids(ImmutableGetByKey.of(ImmutableSet.of("MISSING", "i1"))).isOf("i1");
  }

  @Test
  void multiple() {
    repository.insert(generator.get().withId("i1"));
    repository.insert(generator.get().withId("i2"));

    ids(ImmutableGetByKey.of(Collections.emptySet())).isEmpty();
    ids(ImmutableGetByKey.of(Collections.singleton("MISSING"))).isEmpty();
    ids(ImmutableGetByKey.of(Collections.singleton("i1"))).isOf("i1");
    ids(ImmutableGetByKey.of(Collections.singleton("i2"))).isOf("i2");
    ids(ImmutableGetByKey.of(ImmutableSet.of("i1", "i2"))).hasContentInAnyOrder("i1", "i2");
    ids(ImmutableGetByKey.of(ImmutableSet.of("i1", "MISSING"))).isOf("i1");
  }

  private IterableChecker<List<String>, String> ids(StandardOperations.GetByKey op) {
    List<String> list = Flowable.fromPublisher(session.execute(op).publisher())
            .cast(TypeHolder.StringHolder.class).map(TypeHolder.StringHolder::id)
            .toList().blockingGet();
    return check(list);
  }

}
