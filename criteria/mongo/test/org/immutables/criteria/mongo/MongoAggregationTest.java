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

package org.immutables.criteria.mongo;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.PersonAggregationTest;
import org.junit.Rule;
import org.junit.rules.RuleChain;

public class MongoAggregationTest extends PersonAggregationTest  {

  private final MongoResource mongo = MongoResource.create();
  private final BackendResource backend = new BackendResource(mongo.database());

  @Rule
  public final RuleChain chain= RuleChain.outerRule(mongo).around(backend);

  @Override
  protected Backend backend() {
    return backend.backend();
  }


}
