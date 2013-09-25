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
package org.immutables.generate.internal.javascript;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Closeables;
import java.io.Reader;
import java.net.URI;
import java.util.concurrent.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScript;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;

public class PrecompilingScriptProvider implements ModuleScriptProvider {

  private static final int LEVEL_INTERPRET_UNOPTIMIZED = -1;
  private static final int LEVEL_COMPILE_OPTIMIZED = 9;

  private static final int FIRST_LINE_NUMBER = 1;

  private static final Cache<String, ModuleScript> cache = CacheBuilder.newBuilder().build();
  private final ModuleSourceProvider sourceProvider;
  private final boolean interpret;

  public PrecompilingScriptProvider(ModuleSourceProvider sourceProvider, boolean interpret) {
    this.sourceProvider = sourceProvider;
    this.interpret = interpret;
  }

  @Override
  public ModuleScript getModuleScript(
      final Context cx,
      final String moduleId,
      URI moduleUri,
      URI baseUri,
      final Scriptable paths)
      throws Exception {
    return cache.get(moduleId, new Callable<ModuleScript>() {
      @Override
      public ModuleScript call() throws Exception {
        ModuleSource source = sourceProvider.loadSource(moduleId, paths, null);

        int currentOptimizationLevel = cx.getOptimizationLevel();
        cx.setOptimizationLevel(
            interpret
                ? LEVEL_INTERPRET_UNOPTIMIZED
                : LEVEL_COMPILE_OPTIMIZED);

        Reader reader = source.getReader();
        try {
          return new ModuleScript(
              cx.compileReader(
                  reader,
                  source.getUri().toString(),// use moduleId or uri?
                  FIRST_LINE_NUMBER,
                  source.getSecurityDomain()),
              source.getUri(),
              source.getBase());
        } finally {
          Closeables.close(reader, true);
          cx.setOptimizationLevel(currentOptimizationLevel);
        }
      }
    });
  }

}
