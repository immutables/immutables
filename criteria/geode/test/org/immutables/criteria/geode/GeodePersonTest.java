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
import org.apache.geode.cache.Region;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;

import java.util.EnumSet;
import java.util.Set;

public class GeodePersonTest extends AbstractPersonTest  {

  @ClassRule
  public static final GeodeResource GEODE = GeodeResource.create();

  private static Region<String, Person> region;

  private PersonRepository repository;

  @BeforeClass
  public static void setup() {
    Cache cache = GEODE.cache();
    region =  cache.<String, Person>createRegionFactory()
            .setKeyConstraint(String.class)
            .setValueConstraint(Person.class)
            .create("persons");
  }

  @Before
  public void setUp() throws Exception {
    region.clear();
    repository = new PersonRepository(new GeodeBackend(x -> region));
  }


  @Override
  protected Set<Feature> features() {
    return EnumSet.of(Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT, Feature.ORDER_BY);
  }

  @Ignore
  @Override
  public void nested() {
    // nested doesn't work yet in Geode. Need custom PDX serializer
  }

  @Override
  protected PersonRepository repository() {
    return repository;
  }

}
