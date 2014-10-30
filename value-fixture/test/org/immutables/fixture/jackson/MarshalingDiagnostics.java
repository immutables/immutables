package org.immutables.fixture.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.immutables.fixture.jdbi.RecordMarshaler;

public class MarshalingDiagnostics {

  public static void main(String... args) throws IOException {
    JsonFactory factory = new JsonFactory();
    JsonParser createParser =
        factory.createParser(new ByteArrayInputStream("{ \"name\":1, \"id\":true }".getBytes(StandardCharsets.US_ASCII)));

    // new ObjectMapper().readTree(createParser);
    RecordMarshaler.instance().unmarshalInstance(createParser);
  }
}
