/*
 * Copyright 2026 Immutables Authors and Contributors
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

import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

public class AttributeBuilderReflectionTest {

  @Rule
  public final ProcessorRule rule = new ProcessorRule();

  @Test
  public void attributeBuilderNamedAfterOwnAttribute() {
    check(attributeBuilderName(Alpha.class)).is("alphaInner");
    check(attributeBuilderName(Beta.class)).is("betaInner");
  }

  private String attributeBuilderName(Class<?> type) {
    ValueAttribute attribute = rule.value(type).attributes.get(0);
    return attribute.getAttributeBuilderDescriptor().attributeName();
  }

  @ProcessorRule.TestImmutable
  interface Inner {
    String value();
  }

  @ProcessorRule.TestImmutable
  @Value.Style(attributeBuilderDetection = true)
  interface Alpha {
    Inner alphaInner();
  }

  @ProcessorRule.TestImmutable
  @Value.Style(attributeBuilderDetection = true)
  interface Beta {
    Inner betaInner();
  }
}
