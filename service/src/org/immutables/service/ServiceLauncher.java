/*
    Copyright 2013-2014 Immutables.org authors

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

import org.immutables.service.logging.Logging;
import org.immutables.service.logging.Tracing;
import org.immutables.service.logging.TracingLogEventListener;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Provider;

/**
 * Conventional service launcher that uses unmarshaled configuration files. When unmarshaled,
 * immutable object with configuration will be queried to provide configured Guice modules.
 * To provide modules, unmarshaled configuration should implement {@link Provider}{@link Module
 * &lt;Module&gt;}. Then injector will be constructed from module (possible combined using
 * {@link Modules#combine(Iterable)} and queried
 * for a {@link Set} of {@link Service}s that will be used by {@link ServiceManager} to bootstrap
 * and manage services. Those services may be contributed by individual modules using
 * {@link Multibinder}. There's shutdown hook to close all services in a at most 2 seconds.
 */
@Beta
public final class ServiceLauncher {
  private ServiceLauncher() {}

  static {
    Tracing.init();
  }

  public static void main(String... args) {
    Logging.dispatcher().register(new TracingLogEventListener());
    Injector injector = Guice.createInjector(stage(), loadModule(args[0]));

    final ServiceManager manager = injector.getInstance(ServiceManager.class);

    Runtime.getRuntime().addShutdownHook(new Thread("shutdown") {
      @Override
      public void run() {
        try {
          manager.stopAsync().awaitStopped(2, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
          // stopping timed out
        }
      }
    });

    manager.addListener(new Listener() {
      @Override
      public void failure(Service service) {
        System.err.println(Joiner.on('\n').withKeyValueSeparator(":").join(manager.servicesByState().entries()));
        System.exit(1);
      }
    }, MoreExecutors.sameThreadExecutor());
    manager.startAsync().awaitHealthy();
  }

  private static Module loadModule(String configurationClassName) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(configurationClassName));
    return Configurations.loadModule(
        System.getProperty("conf.path", "/service.conf"),
        configurationClassName);
  }

  private static Stage stage() {
    return Stage.valueOf(System.getProperty("conf.stage", Stage.PRODUCTION.name()));
  }
}
