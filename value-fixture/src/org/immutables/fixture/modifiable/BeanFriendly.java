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
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(
    beanFriendlyModifiables = true,
    deepImmutablesDetection = true,
    create = "new",
    get = {"get*", "is*"})
public interface BeanFriendly extends Identifiable {

  boolean isPrimary();

  @Override
  int getId();

  String getDescription();

  List<String> getNames();

  Map<String, String> getOptions();

  @Nullable
  Mod getMod();

  @Value.Default
  default Mod getDefaultMod() {
    return ImmutableMod.builder().build();
  }

  @Nullable
  @Value.Default
  default Mod getDefaultNullableMod() {
    return ImmutableMod.builder().build();
  }

  @Value.Derived
  default Mod getDerivedMod() {
    return getDefaultMod();
  }

  @Nullable
  @Value.Derived
  default Mod getDerivedNullableMod() {
    return getDefaultMod();
  }

  @Value.Lazy
  default Mod getLazyMod() {
    return getDefaultMod();
  }

  @Nullable
  @Value.Lazy
  default Mod getLazyNullableMod() {
    return getDefaultMod();
  }

  @Nullable
  List<Integer> getExtra();

  @Value.Immutable
  @Value.Modifiable
  public interface Mod {}
}

interface Identifiable {
  int getId();
}
