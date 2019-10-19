/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.immutables.criteria.geode;

import com.google.common.base.Preconditions;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.distributed.AbstractLauncher;
import org.apache.geode.distributed.ServerLauncher;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages embedded Geode instance using native {@link ServerLauncher}.
 */
class GeodeExtension implements ParameterResolver, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(GeodeExtension.class);

  private static final String KEY = "geode";

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    GeodeResource resource = context.getStore(NAMESPACE).get(KEY, GeodeResource.class);
    if (resource != null) {
      for (Region<?, ?> region: resource.cache().rootRegions()) {
        region.destroyRegion();
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return Cache.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return getOrCreate(extensionContext).cache();
  }

  private static GeodeResource getOrCreate(ExtensionContext context) {
    return context.getStore(NAMESPACE).getOrComputeIfAbsent(KEY, key -> new GeodeResource(), GeodeResource.class);
  }

  private static class GeodeResource implements ExtensionContext.Store.CloseableResource {

    private final ServerLauncher launcher;

    private GeodeResource() {
      this.launcher  = new ServerLauncher.Builder()
              .setMemberName("fake-geode")
              .set("log-file", "") // log to stdout
              .set("log-level", "severe") // minimal logging
              .set("bind-address", "127.0.0.1") // accept internal connections only
              .setServerPort(0) // bind to any available port
              .setPdxPersistent(false)
              .setPdxReadSerialized(true)
              .build();

      launcher.start();
    }

    /**
     * Returns current cache instance which was initialized for tests.
     * @throws IllegalStateException if server process didn't start
     */
    Cache cache() {
      requireStatus(AbstractLauncher.Status.ONLINE);
      return CacheFactory.getAnyInstance();
    }

    private void requireStatus(AbstractLauncher.Status expected) {
      final AbstractLauncher.Status current = launcher.status().getStatus();
      Preconditions.checkState(current == expected,
              "Expected state %s but got %s", expected, current);
    }
    @Override
    public void close() {
      if (launcher.status().getStatus() == AbstractLauncher.Status.ONLINE) {
        CacheFactory.getAnyInstance().close();
      }

      final Path pidFile = Paths.get(launcher.getWorkingDirectory()).resolve("vf.gf.server.pid");
      launcher.stop();

      if (Files.exists(pidFile)) {
        // delete PID file. Otherwise ("next") geode instance complains about existing process
        try {
          Files.delete(pidFile);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }
}
