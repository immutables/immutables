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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import java.util.Set;
import javax.inject.Provider;
import org.immutables.common.logging.Logging;
import org.immutables.common.logging.Tracing;
import org.immutables.common.logging.TracingLogEventListener;

/**
 * Conventional service launcher that uses unmarshaled configuration files. When unmarshaled,
 * immutable object with configuration will be queried to provide configured Guice modules.
 * To provide modules, unmarshaled configuration should implement {@link Provider}{@link Module
 * &lt;Module&gt;}. Then injector will be constructed from module (possible combined using
 * {@link Modules#combine(Iterable)} and queried
 * for a {@link Set} of {@link Service}s that will be used by {@link ServiceManager} to bootstrap
 * and manage services. Those services may be contributed by individual modules using
 * {@link Multibinder}.
 */
public class ServiceLauncher {
  static {
    Tracing.init();
  }

  public static void main(String... args) {
    Logging.dispatcher().register(new TracingLogEventListener());
    Injector injector = Guice.createInjector(stage(), loadModule(args[0]));
    ServiceManager manager = injector.getInstance(ServiceManager.class);

    // TODO For now lifecycle is simplistic
    manager.startAsync().awaitHealthy();
    manager.awaitStopped();
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
