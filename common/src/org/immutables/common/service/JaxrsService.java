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
package org.immutables.common.service;

import org.immutables.common.marshal.JaxrsMessageBodyProvider;
import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import static com.google.common.base.Preconditions.*;

/**
 * Combines power of Grizzly, Jersey and Guice to provide embedded JAX-RS 2.0 enpoints over http.
 */
@Beta
@ThreadSafe
public class JaxrsService extends AbstractIdleService {
  static {
    JerseyInjectBridges.installBridgingServiceLocatorGenerator();
  }

  private final Injector injector;
  private final URI uri;
  private HttpServer httpServer;
  private final String[] packagesToScan;

  public JaxrsService(URI uri, Injector injector, String... packagesToScan) {
    this.uri = checkNotNull(uri);
    this.injector = checkNotNull(injector);
    this.packagesToScan = checkNotNull(packagesToScan);
  }

  @Override
  protected void startUp() throws Exception {
    httpServer = GrizzlyHttpServerFactory.createHttpServer(
        uri,
        createApplicationHandler());

//    httpServer.getServerConfiguration()
//        .addHttpHandler(new CLStaticHttpHandler(getClass().getClassLoader()), "/static");

    httpServer.start();
  }

  @Override
  protected void shutDown() throws Exception {
    httpServer.stop();
  }

  private ApplicationHandler createApplicationHandler() {
    if (packagesToScan.length > 0) {
      // Resources from scanned packages will be instantiated by HK2
      // so we need to bridge guice injector
      JerseyInjectBridges.bridgeInjector(injector);
    }
    Set<Object> resourceAndProviderInstances = instantiateResourceAndProviderInstances();
    JerseyInjectBridges.skipInjectionForInstances(resourceAndProviderInstances);

    ResourceConfig resourceConfig =
        new ResourceConfig()
            .packages(packagesToScan)
            .registerInstances(resourceAndProviderInstances)
            .register(JaxrsMessageBodyProvider.class);

    return new ApplicationHandler(resourceConfig);
  }

  private Set<Object> instantiateResourceAndProviderInstances() {
    Set<Object> resouceAndProviderInstances = Sets.newIdentityHashSet();
    collectExplicitResourcesAndProviders(resouceAndProviderInstances);
    return resouceAndProviderInstances;
  }

  private void collectExplicitResourcesAndProviders(Set<Object> resouceAndProviderInstances) {
    Map<Key<?>, Binding<?>> bindings = injector.getBindings();
    for (Binding<?> b : bindings.values()) {
      Key<?> key = b.getKey();
      Class<?> type = key.getTypeLiteral().getRawType();
      if (type.isAnnotationPresent(Path.class) || type.isAnnotationPresent(Provider.class)) {
        resouceAndProviderInstances.add(injector.getInstance(key));
      }
    }
  }
}
