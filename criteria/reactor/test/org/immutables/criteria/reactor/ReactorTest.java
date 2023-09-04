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

package org.immutables.criteria.reactor;

import org.immutables.criteria.Criteria;
import org.immutables.criteria.inmemory.InMemoryBackend;
import org.immutables.criteria.repository.FakeBackend;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ReactorTest {

  @Test
  void empty() {
    ReactorModelRepository repo = new ReactorModelRepository(new FakeBackend(Flux.empty()));
    StepVerifier.create(repo.findAll().fetch()).verifyComplete();
  }

  @Test
  void single() {
    ReactorModelRepository repo = new ReactorModelRepository(new FakeBackend(Flux.just(ImmutableReactorModel.builder().id("id1").build())));
    StepVerifier.create(repo.findAll().fetch()).thenRequest(1).expectNextCount(1).expectComplete().verify();
  }

  /**
   * Validate the projections work with different types of facets (see {@link org.immutables.criteria.repository.Facet}).
   */
  @Test
  void projection() {
    // need in-memory backend because of projections. FakeBackend does not support projections.
    InMemoryBackend backend = new InMemoryBackend();
    ReactorModelRepository repo = new ReactorModelRepository(backend);
    repo.insert(ImmutableReactorModel.builder().id("id1").build()).block();
    StepVerifier.create(repo.findAll().select(ReactorModelCriteria.reactorModel.id).limit(1).offset(0).fetch()).thenRequest(1).expectNext("id1").expectComplete().verify();
  }

  @Test
  void error() {
    ReactorModelRepository repo = new ReactorModelRepository(new FakeBackend(Flux.error(new RuntimeException("boom"))));
    StepVerifier.create(repo.findAll().fetch()).verifyErrorMessage("boom");
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository(facets = {ReactorReadable.class, ReactorWritable.class, ReactorWatchable.class})
  interface ReactorModel {
    @Criteria.Id
    String id();
  }
}
