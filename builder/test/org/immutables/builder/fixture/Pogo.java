/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.builder.fixture;

import java.lang.annotation.RetentionPolicy;
import org.immutables.builder.Builder;

public class Pogo {
  final int a;
  final String b;
  final RetentionPolicy policy;

  @Builder.Constructor
  public Pogo(@Builder.Parameter int a, String b, @Builder.Switch RetentionPolicy policy) {
    this.a = a;
    this.b = b;
    this.policy = policy;
  }

  public static void main(String... args) {
    Pogo pogo = new PogoBuilder(1)
        .b("a")
        .runtimePolicy()
        .build();

    pogo.toString();
  }
}
