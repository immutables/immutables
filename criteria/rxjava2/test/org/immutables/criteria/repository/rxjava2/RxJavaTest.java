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

package org.immutables.criteria.repository.rxjava2;

import io.reactivex.Flowable;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.repository.FakeBackend;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class RxJavaTest {

  @Test
  void empty() {
    RxJavaModelRepository repo = new RxJavaModelRepository(new FakeBackend(Flowable.empty()));
    repo.findAll().fetch().test().awaitDone(1, TimeUnit.SECONDS).assertNoValues();
  }

  @Test
  void single() {
    RxJavaModelRepository repo = new RxJavaModelRepository(new FakeBackend(Flowable.just(ImmutableRxJavaModel.builder().id("id1").build())));
    repo.findAll().fetch().test().awaitDone(1, TimeUnit.SECONDS).assertValueCount(1);
  }

  /**
   * Validate the projections work with different types of facets (see {@link org.immutables.criteria.repository.Facet}).
   */
  @Test
  void projection() {
    // TODO: can't use InMemoryBackend because of circular dependency.
    RxJavaModelRepository repo = new RxJavaModelRepository(new FakeBackend(Flowable.just(ImmutableRxJavaModel.builder().id("id1").build())));
    repo.findAll()
            //.select(RxJavaModelCriteria.rxJavaModel.id)  TODO: FakeBackend does not support projections
            .limit(1)
            .offset(0)
            .fetch()
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(1);
  }

  @Test
  void error() {
    RxJavaModelRepository repo = new RxJavaModelRepository(new FakeBackend(Flowable.error(new RuntimeException("boom"))));
    repo.findAll().fetch().test().awaitDone(1, TimeUnit.SECONDS).assertErrorMessage("boom");
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository(facets = {RxJavaReadable.class, RxJavaWritable.class, RxJavaWatchable.class})
  interface RxJavaModel {
    @Criteria.Id
    String id();
  }
}
