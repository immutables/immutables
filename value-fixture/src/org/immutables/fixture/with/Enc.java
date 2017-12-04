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
package org.immutables.fixture.with;

import javax.annotation.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import com.google.common.collect.Multimap;
import org.immutables.value.Value;

@JsonDeserialize
@Value.Enclosing
public interface Enc {
  // disabling builder and copy will have no effect on on-demand generation
  @Value.Immutable(builder = false, copy = false)
  interface Suppied<T extends Number> extends ImmutableEnc.WithSuppied<T> {
    String a();

    T num();

    Multimap<String, T> mm();

    Optional<T> opt();

    int[] array();

    @Nullable
    String[] nularr();

    class Builder<T extends Number> extends ImmutableEnc.Suppied.Builder<T> {}
  }

  @SuppressWarnings("CheckReturnValue")
  static void use() {
    new Suppied.Builder<Long>()
        .a("a")
        .num(0b1111L)
        .build();
  }
}
