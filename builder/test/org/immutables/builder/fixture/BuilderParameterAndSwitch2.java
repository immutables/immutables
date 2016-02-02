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
import org.immutables.value.Value;

@Value.Style(typeInnerBuilder = "Building")
@Value.Immutable
public interface BuilderParameterAndSwitch2 {
  @Builder.Parameter
  int theory();

  @Builder.Parameter
  String value();

  @Builder.Switch
  RetentionPolicy policy();

  class Building extends ImmutableBuilderParameterAndSwitch2.Builder {
    public Building() {
      super(0, "");
    }

    public Building(int theory, String label) {
      super(theory, label);
    }
  }
}
