package org.immutables.gson.stream;

import com.fasterxml.jackson.core.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.IOException;

/**
 *  {@link JsonReader} implementation backed by Jackson's {@link JsonParser}.
 *  This version assumes the token producer only supports strings, therefore will
 *  work with the XML and properties formats.
 */
public class XmlParserReader extends JsonParserReader {
  public XmlParserReader(JsonParser parser) {
    super(parser);
  }

  @Override
  public boolean nextBoolean() throws IOException {
    return Boolean.parseBoolean(nextString());
  }

  @Override
  public double nextDouble() throws IOException {
    return Double.parseDouble(nextString());
  }

  @Override
  public long nextLong() throws IOException {
    return Long.parseLong(nextString());
  }

  @Override
  public int nextInt() throws IOException {
    return Integer.parseInt(nextString());
  }
}
