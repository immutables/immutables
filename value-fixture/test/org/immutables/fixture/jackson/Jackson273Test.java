package org.immutables.fixture.jackson;

import static org.immutables.check.Checkers.check;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class Jackson273Test {
  private static String SAMPLE_JSON = "{\"organizationId\": 172}";

  @Test
  public void serialize() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ProjectInformation info = mapper.readValue(SAMPLE_JSON, ProjectInformation.class);
    check(info.getOrganizationId()).is(172);
  }
}
