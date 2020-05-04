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

package org.immutables.criteria.inmemory;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.AbstractPersonTest;

import java.util.EnumSet;
import java.util.Set;

public class InMemoryPersonTest extends AbstractPersonTest  {

  private final InMemoryBackend backend = new InMemoryBackend();

  @Override
  protected Set<Feature> features() {
    return EnumSet.of(Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT,
            Feature.ORDER_BY,
            Feature.QUERY_WITH_PROJECTION,
            Feature.QUERY_WITH_OFFSET, Feature.REGEX,
            Feature.DELETE_BY_QUERY,
            Feature.STRING_EMPTY,
            Feature.STRING_PREFIX_SUFFIX,
            Feature.ITERABLE_SIZE,
            Feature.ITERABLE_CONTAINS,
            Feature.STRING_LENGTH);
  }

  @Override
  protected Backend backend() {
    return backend;
  }

}
