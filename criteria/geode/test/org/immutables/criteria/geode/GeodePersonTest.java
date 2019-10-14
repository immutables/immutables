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

import org.apache.geode.cache.Cache;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WithSessionCallback;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.Set;

@ExtendWith(GeodeExtension.class)
public class GeodePersonTest extends AbstractPersonTest  {

  private final Backend backend;

  public GeodePersonTest(Cache cache) {
    AutocreateRegion autocreate = new AutocreateRegion(cache);
    backend = WithSessionCallback.wrap(new GeodeBackend(GeodeSetup.of(cache)), autocreate);
  }

  @Override
  protected Set<Feature> features() {
    return EnumSet.of(Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT, Feature.ORDER_BY, Feature.QUERY_WITH_PROJECTION);
  }

  @Override
  protected Backend backend() {
    return backend;
  }

  @Disabled
  @Override
  public void nested() {
    // nested doesn't work yet in Geode. Need custom PDX serializer
  }

}
