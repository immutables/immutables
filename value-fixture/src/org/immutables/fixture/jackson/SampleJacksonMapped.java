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
package org.immutables.fixture.jackson;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Jackson;
import org.immutables.value.Json;
import org.immutables.value.Value;

@Value.Immutable
@Json.Marshaled
@Jackson.Mapped
@JsonDeserialize(as = ImmutableSampleJacksonMapped.class)
public interface SampleJacksonMapped {
  String a();

  List<Integer> b();

  Optional<RegularPojo> pojo();

  public class RegularPojo {
    public int x, y;

    @Override
    public int hashCode() {
      return Objects.hashCode(x, y);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof RegularPojo) {
        return ((RegularPojo) obj).x == x
            && ((RegularPojo) obj).y == y;
      }
      return false;
    }
  }
}
