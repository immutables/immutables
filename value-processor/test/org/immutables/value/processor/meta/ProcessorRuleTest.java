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

package org.immutables.value.processor.meta;

import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.immutables.check.Checkers.check;

/**
 * Test of {@link ProcessorRule}
 */
public class ProcessorRuleTest {

  @Rule
  public final ProcessorRule rule = new ProcessorRule();

  @Test
  public void basic() {
    ValueType type = rule.value(MyClass.class);
    check(type.attributes).hasSize(1);
    check(type.attributes.get(0).name()).is("aaa");
    check(type.attributes.get(0).getType()).is("java.util.List<java.lang.String>");
  }

  @ProcessorRule.TestImmutable
  interface MyClass {
    List<String> aaa();
  }

}
