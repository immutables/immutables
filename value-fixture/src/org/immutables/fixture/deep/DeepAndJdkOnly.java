/*
   Copyright 2017 Immutables Authors and Contributors

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
package org.immutables.fixture.deep;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
    deepImmutablesDetection = true,
    jdkOnly = true)
@interface DeepStyle {}

public interface DeepAndJdkOnly {

  @DeepStyle
  @Value.Immutable
  interface Deep {
    @Value.Parameter
    int a();

    @Value.Parameter
    int b();
  }

  @DeepStyle
  @Value.Immutable
  interface Container {
    Deep getDeep();
  }

  // Compile validation of generation of immutable return type and builder initializer by
  // constructor-args .
  static void use() {
    ImmutableContainer c = ImmutableContainer.builder().deep(1, 2).build();
    ImmutableDeep deep = c.getDeep();
    deep.withA(3).withB(4);
  }
}
