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

import com.mongodb.reactivestreams.client.MongoDatabase;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.typemodel.BigDecimalTemplate;
import org.immutables.criteria.typemodel.BooleanTemplate;
import org.immutables.criteria.typemodel.DateTemplate;
import org.immutables.criteria.typemodel.DoubleTemplate;
import org.immutables.criteria.typemodel.EnumTemplate;
import org.immutables.criteria.typemodel.IntegerTemplate;
import org.immutables.criteria.typemodel.LocalDateTemplate;
import org.immutables.criteria.typemodel.LocalDateTimeTemplate;
import org.immutables.criteria.typemodel.LongTemplate;
import org.immutables.criteria.typemodel.StringTemplate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MongoExtension.class)
class MongoIntegrationTest {

  private final Backend backend;

  MongoIntegrationTest(MongoDatabase database) {
    this.backend = new BackendResource(database).backend();
  }

  @Nested
  class String extends StringTemplate {
    private String() {
      super(backend);
    }
  }

  @Nested
  class BooleanTest extends BooleanTemplate {
    private BooleanTest() {
      super(backend);
    }
  }

  @Nested
  class LocalDateTest extends LocalDateTemplate {
    private LocalDateTest() {
      super(backend);
    }
  }

  @Nested
  class LocalDateTimeTest extends LocalDateTimeTemplate {
    private LocalDateTimeTest() {
      super(backend);
    }
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
  }

}
