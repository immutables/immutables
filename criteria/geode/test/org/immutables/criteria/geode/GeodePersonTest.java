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
import org.immutables.criteria.backend.ContainerNaming;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.Person;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.Set;

@ExtendWith(GeodeExtension.class)
public class GeodePersonTest extends AbstractPersonTest  {

  private final GeodeBackend backend;

  public GeodePersonTest(Cache cache) {
    // create region
    cache.<String, Person>createRegionFactory()
            .setKeyConstraint(String.class)
            .setValueConstraint(Person.class)
            .create(ContainerNaming.DEFAULT.name(Person.class));

    this.backend =  new GeodeBackend(GeodeSetup.of(cache));
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
