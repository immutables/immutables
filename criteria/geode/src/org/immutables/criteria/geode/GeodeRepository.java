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

package org.immutables.criteria.geode;

import org.apache.geode.cache.Region;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.Repository;
import org.reactivestreams.Publisher;

import java.util.Objects;

public class GeodeRepository<T> implements Repository<T> {

  private final Region<?, ?> region;

  public GeodeRepository(Region<?, ?> region) {
    this.region = Objects.requireNonNull(region, "cache is null");
  }

  @Override
  public Publisher<T> query(DocumentCriteria<T> criteria) {
    throw new UnsupportedOperationException();
  }

}
