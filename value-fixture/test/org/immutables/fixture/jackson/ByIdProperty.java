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
package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

// regression test for rarely used functionality
// which was broken once
public class ByIdProperty {
  static class ByidInstanceResolver extends SimpleObjectIdResolver {
    @Override
    public ObjectIdResolver newForDeserialization(Object context) {
      return new ByidInstanceResolver();
    }

    @Override
    public Object resolveId(IdKey id) {
      return ImmutableByid.builder()
          .id(id.key.toString())
          .build();
    }
  }

  @JsonSerialize(as = ImmutableAsis.class)
  @JsonDeserialize(as = ImmutableAsis.class)
  @Value.Immutable
  public interface Asis {
    @JsonIdentityReference(alwaysAsId = true)
    @JsonIdentityInfo(
        resolver = ByidInstanceResolver.class,
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
    Byid b();
  }
  
  @JsonSerialize(as = ImmutableByid.class)
  @JsonDeserialize(as = ImmutableByid.class)
  @Value.Immutable
  public interface Byid {
    String getId();
  }


  @Test
  public void roundtrip() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    ImmutableAsis asis = ImmutableAsis.builder()
        .b(ImmutableByid.builder()
            .id("abc")
            .build())
        .build();

    String json = mapper.writeValueAsString(asis);

    check(mapper.readValue(json, ImmutableAsis.class)).is(asis);
  }
}
