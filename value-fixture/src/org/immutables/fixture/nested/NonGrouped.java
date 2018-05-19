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
package org.immutables.fixture.nested;

import javax.annotation.Nullable;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
public abstract class NonGrouped {

  @Value.Immutable
  abstract static class Abra {}

  @Value.Immutable(builder = false)
  public interface Cadabra {}

  @Value.Immutable(builder = false)
  abstract static class Dabra {
    @Value.Parameter
    abstract int dabra();
  }

  @Value.Immutable(builder = false)
  public abstract static class Buagra {
    @Value.Parameter
    @Nullable
    public abstract String buagra();
  }
}
