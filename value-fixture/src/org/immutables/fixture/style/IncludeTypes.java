/*
    Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.fixture.style;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import org.immutables.value.Value;

/**
 * Feature combination
 * <ul>
 * <li>Included on types and packages
 * <li>Included as nested types
 * <li>Package style application
 * </ul>
 */
@Value.Include(Serializable.class)
@Value.Immutable
public class IncludeTypes {

  void use() {
    // this immutable type
    ImmutableIncludeTypes.of();
    // included on this type
    ImmutableSerializable.of();
    // included on package
    ImmutableTicker.builder().read(1).build();

    // included in IncludeNestedTypes
    ImmutableIncludeNestedTypes.Retention retention =
        ImmutableIncludeNestedTypes.Retention.builder()
            .value(RetentionPolicy.CLASS)
            .build();

    // included in IncludeNestedTypes
    ImmutableIncludeNestedTypes.Target target =
        ImmutableIncludeNestedTypes.Target.builder()
            .value(ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE)
            .build();

    // package applied style "copyWith*" test
    // see PackageStyle
    retention.copyWithValue(RetentionPolicy.RUNTIME);
    target.copyWithValue(ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE);
  }
}
