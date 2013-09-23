package org.immutables.common.web;

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

public class HttpServiceTest {
  String uri = "http://localhost:9996";
  private HttpJaxrsService service;

  @Before
  public void setup() {
    Injector injector = Guice.createInjector(new SillyWebModule());

    service = new HttpJaxrsService(URI.create(uri), injector);
    service.startAndWait();
  }

  @After
  public void teardown() {
    service.stopAndWait();
  }

  @Test
  public void makePostJson() {
    Client client = ClientBuilder.newBuilder()
        .register(MarshaledMessageBodyProvider.class)
        .build();

    Response response = client.target(uri)
        .path("/res")
        .request()
        .post(Entity.json(ImmutableSillySub3.builder().build()));

    SillyDumb entity = response.readEntity(SillyDumb.class);
    check(entity.c3()).isOf(1);
  }
}
