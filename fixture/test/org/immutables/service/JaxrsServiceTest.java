/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.service;

import org.immutables.service.testscan.SillyTopLevelResource2;
import org.immutables.common.marshal.JaxrsMessageBodyProvider;
import org.immutables.service.JaxrsService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.immutables.generate.silly.ImmutableSillySub3;
import org.immutables.generate.silly.SillyDumb;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class JaxrsServiceTest {
  String scanPackage = SillyTopLevelResource2.class.getPackage().getName();
  String uri = "http://localhost:9996";
  JaxrsService service;

  Injector injector = Guice.createInjector(new SillyWebModule());

  Client client = ClientBuilder.newBuilder()
      .register(JaxrsMessageBodyProvider.class)
      .build();

  @Before
  public void setup() {
    service = new JaxrsService(URI.create(uri), injector, scanPackage);
    service.startAsync().awaitRunning();
  }

  @After
  public void teardown() {
    service.stopAsync().awaitTerminated();
  }

  @Test
  public void postAndMarshaling() {
    SillyDumb response = client.target(uri)
        .path("/res")
        .request()
        .post(Entity.json(ImmutableSillySub3.builder().build()), SillyDumb.class);

    check(response.c3()).isOf(1);
  }

  @Test
  public void getAndParameterInjection() {
    Response response = client.target(uri)
        .path("/res")
        .request()
        .get();

    check(response.getStatus()).is(200);
    check(response.readEntity(String.class)).not("null");
  }

  @Test
  public void getAndGuiceBridgeInjection() {
    Response response = client.target(uri)
        .path("/res2")
        .request()
        .get();

    check(response.getStatus()).is(200);
    check(response.readEntity(String.class)).not("null");
  }
}
