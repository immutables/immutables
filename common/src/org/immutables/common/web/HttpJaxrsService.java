package org.immutables.common.web;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;

public class HttpJaxrsService extends AbstractIdleService {
  static {
    JerseyInjectHooks.installInjectionSkippingCapableServiceLocatorGenerator();
  }

  private final Injector injector;
  private final URI uri;
  private HttpServer httpServer;

  public HttpJaxrsService(URI uri, Injector injector) {
    this.uri = uri;
    this.injector = injector;
  }

  private URI uri() {
    return uri;
  }

  @Override
  protected void startUp() throws Exception {
    httpServer =
        GrizzlyHttpServerFactory.createHttpServer(
            uri(),
            createApplicationHandler());

    httpServer.start();
  }

  @Override
  protected void shutDown() throws Exception {
    httpServer.stop();
  }

  private ApplicationHandler createApplicationHandler() throws Exception {

//    Set<Object> resourceInstances =
//        injector.getInstance(Key.get(new TypeLiteral<Set<Object>>() {}, Path.class));
//
//    Set<Object> providerInstances =
//        injector.getInstance(Key.get(new TypeLiteral<Set<Object>>() {}, Provider.class));

    Set<Object> resourceAndProviderInstances = instantiateResourceAndProviderInstances();

    JerseyInjectHooks.skipInjectionForInstances(resourceAndProviderInstances);

    ResourceConfig resourceConfig =
        new ResourceConfig()
            .registerInstances(resourceAndProviderInstances)
            .register(MarshaledMessageBodyProvider.class);

    return new ApplicationHandler(resourceConfig);
  }

  private Set<Object> instantiateResourceAndProviderInstances() {
    Set<Object> resouceAndProviderInstances = Sets.newIdentityHashSet();

    Map<Key<?>, Binding<?>> bindings = injector.getBindings();
    for (Binding<?> b : bindings.values()) {
      Key<?> key = b.getKey();

      Class<?> type = key.getTypeLiteral().getRawType();
      if (type.isAnnotationPresent(Path.class) || type.isAnnotationPresent(Provider.class)) {
        resouceAndProviderInstances.add(injector.getInstance(key));
      }
    }

    return resouceAndProviderInstances;
  }

}
