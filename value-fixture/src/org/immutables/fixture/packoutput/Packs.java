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
package org.immutables.fixture.packoutput;

import org.immutables.mongo.Mongo;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

// Compilation test for customization of output package
// Tries to use many different generators
@Value.Style(packageGenerated = "b*.impl")
@Value.Immutable
@Value.Enclosing
@Gson.TypeAdapters
@Mongo.Repository
public abstract class Packs {
  public abstract Perk perk();

  @Value.Immutable
  public interface Perk {}

  @SuppressWarnings("CheckReturnValue")
  void use() {
    borg.immutables.fixture.packoutput.impl.ImmutablePacks.builder().build();
    borg.immutables.fixture.packoutput.impl.ImmutablePacks.Perk.builder().build();
    borg.immutables.fixture.packoutput.impl.PacksRepository.class.toString();
    borg.immutables.fixture.packoutput.impl.GsonAdaptersPacks.class.toString();
  }
}
