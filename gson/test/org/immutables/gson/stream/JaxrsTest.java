/*
    Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.gson.stream;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class JaxrsTest {

  private static final GsonMessageBodyProvider PURE_GSON_TEXT_PLAIN =
      new GsonMessageBodyProvider(
          new GsonProviderOptionsBuilder()
              .gson(new Gson())
              .addMediaTypes(MediaType.TEXT_PLAIN_TYPE)
              .allowJackson(false)
              .lenient(true)
              .build()) {};

  private static final URI SERVER_URI = URI.create("http://localhost:8997");
  private static HttpServer httpServer;
  private static Client client;

  @BeforeClass
  public static void setup() throws IOException {
    httpServer = GrizzlyHttpServerFactory.createHttpServer(
        SERVER_URI, createResourceConfig(), false);

    httpServer.start();

    client = ClientBuilder.newBuilder()
        .register(GsonMessageBodyProvider.class)
        .register(PURE_GSON_TEXT_PLAIN)
        .build();
  }

  private static ResourceConfig createResourceConfig() {
    return new ResourceConfig()
        .register(Resource.class)
        .register(GsonMessageBodyProvider.class)
        .register(PURE_GSON_TEXT_PLAIN);
  }

  @AfterClass
  public static void teardown() {
    httpServer.shutdown();
  }

  @Test
  public void gsonJacksonRoundtrip() {

    List<String> result = client.target(SERVER_URI)
        .path("/")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .accept(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.json(Collections.singletonList(13)), new GenericType<List<String>>() {});

    check(result).isOf("a", "b", "c", "[13]");
  }

  @Test
  public void pureGsonRoundtrip() {

    List<String> result = client.target(SERVER_URI)
        .path("/")
        .request(MediaType.TEXT_PLAIN_TYPE)
        .accept(MediaType.TEXT_PLAIN_TYPE)
        .post(Entity.text(Collections.singletonList("11")), new GenericType<List<String>>() {});

    check(result).isOf("x", "y", "[11]");
  }
}
