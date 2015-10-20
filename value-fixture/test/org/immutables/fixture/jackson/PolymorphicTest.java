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
