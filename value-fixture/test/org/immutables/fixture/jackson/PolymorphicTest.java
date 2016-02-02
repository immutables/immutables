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

import static org.immutables.check.Checkers.*;
import org.immutables.fixture.jackson.PolymorphicMappings.DatasetLocator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.immutables.fixture.jackson.PolymorphicMappings.DatasetIdLocator;
import org.junit.Test;

public class PolymorphicTest {
  ObjectMapper om = new ObjectMapper();

  @Test
  public void polymorphic() throws IOException {
    DatasetIdLocator locator = new DatasetIdLocator.Builder().datasetId("id").build();
    DatasetLocator value = om.readValue(om.writeValueAsBytes(locator), DatasetLocator.class);
    check(value).isA(DatasetIdLocator.class);
    check(((DatasetIdLocator) value).getDatasetId()).is(locator.getDatasetId());
  }
}
