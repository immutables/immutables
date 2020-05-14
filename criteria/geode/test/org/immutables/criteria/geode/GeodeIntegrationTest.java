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
import org.immutables.criteria.typemodel.BigDecimalTemplate;
import org.immutables.criteria.typemodel.BooleanLogicTemplate;
import org.immutables.criteria.typemodel.BooleanTemplate;
import org.immutables.criteria.typemodel.CompositeTemplate;
import org.immutables.criteria.typemodel.CountTemplate;
import org.immutables.criteria.typemodel.DateTemplate;
import org.immutables.criteria.typemodel.DeleteByKeyTemplate;
import org.immutables.criteria.typemodel.DistinctLimitCountTemplate;
import org.immutables.criteria.typemodel.DoubleTemplate;
import org.immutables.criteria.typemodel.EnumTemplate;
import org.immutables.criteria.typemodel.GetByKeyTemplate;
import org.immutables.criteria.typemodel.InstantTemplate;
import org.immutables.criteria.typemodel.IntegerTemplate;
import org.immutables.criteria.typemodel.LocalDateTemplate;
import org.immutables.criteria.typemodel.LocalDateTimeTemplate;
import org.immutables.criteria.typemodel.LongTemplate;
import org.immutables.criteria.typemodel.StringTemplate;
import org.immutables.criteria.typemodel.WriteTemplate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

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

    @Disabled("optionals don't work well in Geode yet (pdx serialization)")
    @Override
    protected void optional() {}
  }

  @Nested
  class BooleanLogicTest extends BooleanLogicTemplate {
    private BooleanLogicTest() {
      super(backend);
    }
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
  class LocalDateTimeTest extends LocalDateTimeTemplate {
    private LocalDateTimeTest() {
      super(backend);
    }

    @Disabled
    @Override
    protected void optional() {}
  }

  @Nested
  class InstantTest extends InstantTemplate {
    private InstantTest() {
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

  @Nested
  class EnumTest extends EnumTemplate {
    private EnumTest() {
      super(backend);
    }
  }

  @Nested
  class DoubleTest extends DoubleTemplate {
    private DoubleTest() {
      super(backend);
    }
  }

  @Nested
  class IntegerTest extends IntegerTemplate {
    private IntegerTest() {
      super(backend);
    }
  }

  @Nested
  class BigDecimalTest extends BigDecimalTemplate {
    private BigDecimalTest() {
      super(backend);
    }
  }

  @Nested
  class DateTest extends DateTemplate {
    private DateTest() {
      super(backend);
    }

    @Disabled
    @Override
    protected void optional() {}
  }

  @Nested
  class Write extends WriteTemplate {
    private Write() {
      super(backend);
    }
  }

  @Nested
  class Count extends CountTemplate {
    private Count() {
      super(backend);
    }
  }

  @Nested
  class DistinctLimitCount extends DistinctLimitCountTemplate {
    private DistinctLimitCount() {
      super(backend);
    }
  }

  @Nested
  class Composite extends CompositeTemplate {
    private Composite() {
      super(backend);
    }

    @Disabled("optionals don't yet work in Geode backend")
    @Override
    protected void optional() {
    }
  }

  @Nested
  class GetByKey extends GetByKeyTemplate {
    private GetByKey() {
      super(backend);
    }
  }

  @Nested
  class DeleteByKey extends DeleteByKeyTemplate {
    private DeleteByKey() {
      super(backend);
    }
  }

}

