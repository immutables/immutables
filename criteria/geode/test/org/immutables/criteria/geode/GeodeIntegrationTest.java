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
import org.immutables.criteria.backend.WithSessionCallback;
import org.immutables.criteria.typemodel.BooleanTemplate;
import org.immutables.criteria.typemodel.LocalDateTemplate;
import org.immutables.criteria.typemodel.LongTemplate;
import org.immutables.criteria.typemodel.StringTemplate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Consumer;

@ExtendWith(GeodeExtension.class)
class GeodeIntegrationTest {

  private final Backend backend;

  GeodeIntegrationTest(Cache cache) {
    AutocreateRegion autocreate = new AutocreateRegion(cache);
    backend = WithSessionCallback.wrap(new GeodeBackend(GeodeSetup.of(cache)), autocreate);
  }

  @Nested
  class StringTest extends StringTemplate {
    private StringTest() {
      super(backend);
    }

    @Disabled
    @Override
    protected void startsWith() {}

    @Disabled
    @Override
    protected void endsWith() {}

    @Disabled
    @Override
    protected void contains() {}

    @Disabled("optionals don't work well in Geode yet (pdx serialization)")
    @Override
    protected void optional() {}

    @Disabled("TODO: for some reason Geode throws NPE")
    @Override
    protected void projection() {}
  }

  @Nested
  class BooleanTest extends BooleanTemplate {
    private BooleanTest() {
      super(backend);
    }

    @Disabled("optionals don't work well in Geode yet (pdx serialization)")
    @Override
    protected void optional() {}
  }

  @Nested
  class LocalDateTest extends LocalDateTemplate {
    private LocalDateTest() {
      super(backend);
    }

    @Disabled
    @Override
    protected void optional() {}
  }

  @Nested
  class LongTest extends LongTemplate {
    private LongTest() {
      super(backend);
    }
  }


  private static class AutocreateRegion implements Consumer<Class<?>> {

    private final Cache cache;
    private final ContainerNaming naming;

    private AutocreateRegion(Cache cache) {
      this.cache = cache;
      this.naming = ContainerNaming.DEFAULT;
    }

    @Override
    public void accept(Class<?> entity) {
      String name = naming.name(entity);
      // exists ?
      if (cache.getRegion(name) != null) {
        return;
      }

      // if not, create
      cache.createRegionFactory()
              .setValueConstraint((Class<Object>) entity)
              .create(name);
    }
  }


}

