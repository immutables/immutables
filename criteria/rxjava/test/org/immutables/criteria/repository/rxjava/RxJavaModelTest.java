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

package org.immutables.criteria.repository.rxjava;

import io.reactivex.Flowable;
import org.immutables.criteria.repository.FakeBackend;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class RxJavaModelTest {

  @Test
  public void rxjava() throws InterruptedException {
    RxJavaModelRepository repo = new RxJavaModelRepository(new FakeBackend(Flowable.empty()));
    repo.findAll().fetch().test().awaitDone(1, TimeUnit.SECONDS).assertNoValues();
  }

  @Test
  public void error() {
    RxJavaModelRepository repo = new RxJavaModelRepository(new FakeBackend(Flowable.error(new RuntimeException("boom"))));
    repo.findAll().fetch().test().awaitDone(1, TimeUnit.SECONDS).assertErrorMessage("boom");
  }
}