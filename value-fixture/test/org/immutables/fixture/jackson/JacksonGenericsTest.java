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

import static org.immutables.check.Checkers.check;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class JacksonGenericsTest {
  static class Abc {
    public ImmutableJacksonGenerics<Double> cba = ImmutableJacksonGenerics.<Double>builder()
        .attr(1.0)
        .nm("mn")
        .build();
  }

  @Test
  public void rountrip() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Abc abc = new Abc();
    String json = mapper.writeValueAsString(abc);
    System.out.println(json);
    Abc value = mapper.readValue(json, Abc.class);

    check(value.cba).is(abc.cba);
  }
}
