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

package org.immutables.criteria.elasticsearch;

import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;

class MappingsTest {

  @Test
  void basic() {
    Mapping mapping = Mappings.of(TypeHolder.StringHolder.class);
    check(mapping.fields().get("value").name()).is("keyword");
    check(mapping.fields().get("nullable").name()).is("keyword");
    check(mapping.fields().get("optional").name()).is("keyword");
  }

  @Test
  void longHolder() {
    Mapping mapping = Mappings.of(TypeHolder.LongHolder.class);
    check(mapping.fields().get("value").name()).is("long");
    check(mapping.fields().get("boxed").name()).is("long");
    check(mapping.fields().get("optional").name()).is("long");
    check(mapping.fields().get("nullable").name()).is("long");
  }

  @Test
  void doubleHolder() {
    Mapping mapping = Mappings.of(TypeHolder.DoubleHolder.class);
    check(mapping.fields().get("value").name()).is("double");
    check(mapping.fields().get("boxed").name()).is("double");
    check(mapping.fields().get("optional").name()).is("double");
    check(mapping.fields().get("nullable").name()).is("double");
  }

  @Test
  void integerHolder() {
    Mapping mapping = Mappings.of(TypeHolder.IntegerHolder.class);
    check(mapping.fields().get("value").name()).is("integer");
    check(mapping.fields().get("boxed").name()).is("integer");
    check(mapping.fields().get("optional").name()).is("integer");
    check(mapping.fields().get("nullable").name()).is("integer");
  }

  @Test
  void bigDecimalHolder() {
    Mapping mapping = Mappings.of(TypeHolder.BigDecimalHolder.class);
    check(mapping.fields().get("value").name()).is("double");
    check(mapping.fields().get("optional").name()).is("double");
    check(mapping.fields().get("nullable").name()).is("double");
  }

  @Test
  void bigIntegerHolder() {
    Mapping mapping = Mappings.of(TypeHolder.BigIntegerHolder.class);
    check(mapping.fields().get("value").name()).is("long");
    check(mapping.fields().get("optional").name()).is("long");
    check(mapping.fields().get("nullable").name()).is("long");
  }
}