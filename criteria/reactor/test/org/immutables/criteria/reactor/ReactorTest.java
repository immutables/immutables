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
    ReactorModelRepository repo = new ReactorModelRepository(new FakeBackend(Flux.just(ImmutableReactorModel.builder().build())));
    StepVerifier.create(repo.findAll().fetch()).thenRequest(1).expectNextCount(1).expectComplete().verify();
  }

  @Test
  void error() {
    ReactorModelRepository repo = new ReactorModelRepository(new FakeBackend(Flux.error(new RuntimeException("boom"))));
    StepVerifier.create(repo.findAll().fetch()).verifyErrorMessage("boom");
  }

  @Value.Immutable
  @Criteria
  @Criteria.Repository(facets = {ReactorReadable.class, ReactorWritable.class, ReactorWatchable.class})
  interface ReactorModel {}
}