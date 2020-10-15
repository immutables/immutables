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
package org.immutables.fixture.modifiable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(jpaFriendlyModifiables = true, deepImmutablesDetection = true, create = "new", get = { "get*", "is*" })
public interface JpaFriendly {

  boolean isPrimary();

  int getId();

  String getDescription();

  List<String> getNames();

  Set<String> getScopes();

  Map<String, String> getOptions();

  @Nullable
  JpaMod getMod();

  @Value.Default
  default JpaMod getDefaultMod() {
    return ImmutableJpaMod.builder().build();
  }

  @Nullable
  @Value.Default
  default JpaMod getDefaultNullableMod() {
    return ImmutableJpaMod.builder().build();
  }

  @Value.Derived
  default JpaMod getDerivedMod() {
    return getDefaultMod();
  }

  @Nullable
  @Value.Derived
  default JpaMod getDerivedNullableMod() {
    return getDefaultMod();
  }

  @Value.Lazy
  default JpaMod getLazyMod() {
    return getDefaultMod();
  }

  @Nullable
  @Value.Lazy
  default JpaMod getLazyNullableMod() {
    return getDefaultMod();
  }

  @Nullable
  List<Integer> getExtra();

  @Nullable
  Set<String> getTargets();

  @Value.Immutable
  @Value.Modifiable
  public interface JpaMod {}
}
