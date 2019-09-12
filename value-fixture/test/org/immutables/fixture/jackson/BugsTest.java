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
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class BugsTest {
  private static String SAMPLE_JSON = "{\"organizationId\": 172}";
  ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());

  @Test
  public void deserialize368() throws Exception {
    DefaultCollection defc = mapper.readValue("{\"defnullable\":[0]}", DefaultCollection.class);
    check(defc.defaults()).isOf("");
    check(defc.nullable()).isNull();
    check(defc.defnullable()).isOf(0);
  }

  @Test
  public void serialize273() throws Exception {
    ProjectInformation info = mapper.readValue(SAMPLE_JSON, ProjectInformation.class);
    check(info.getOrganizationId()).is(172);
  }

  // we are testing generation for JsonProperty annotations and checking that they are read
  // and writen the same way
  @Test
  public void roundtrip328() throws Exception {
    String json = mapper.writeValueAsString(ImmutableAttributeIs.builder()
        .isEmpty(false)
        .empty(true)
        .build());

    AttributeIs info = mapper.readValue(json, AttributeIs.class);
    check(!info.isEmpty());
    check(info.getEmpty());
  }

  @Test
  public void roundtrip353() throws Exception {
    ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    String json = mapper.writeValueAsString(ImmutableNamingStrategy.builder()
        .abraCadabra(1)
        .focusPocus(true)
        .build());

    NamingStrategy info = mapper.readValue(json, NamingStrategy.class);
    check(info.abraCadabra()).is(1);
    check(info.focusPocus());

    check(json).is("{'abra_cadabra':1,'focus_pocus':true}".replace('\'', '"'));
  }
}
