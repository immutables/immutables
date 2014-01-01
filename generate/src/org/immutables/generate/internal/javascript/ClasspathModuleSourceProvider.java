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
package org.immutables.generate.internal.javascript;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;

public class ClasspathModuleSourceProvider implements ModuleSourceProvider {

  private static final Object NULL_SECURITY_DOMAIN = null;

  private final ClassLoader classLoader;

  public ClasspathModuleSourceProvider(Class<?> originClass) {
    this(originClass.getClassLoader());
  }

  public ClasspathModuleSourceProvider(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public URL getExistingResource(String path) throws FileNotFoundException {
    URL resource = classLoader.getResource(path);
    if (resource == null) {
      throw new FileNotFoundException(path);
    }
    return resource;
  }

  @Override
  public ModuleSource loadSource(
      String moduleId,
      Scriptable paths,
      Object validator) throws IOException, URISyntaxException {

    String path = toPath(moduleId);
    URL resource = getExistingResource(path);

    InputSupplier<InputStreamReader> readerSupplier =
        Resources.newReaderSupplier(resource, Charsets.UTF_8);

    return new ModuleSource(
        readerSupplier.getInput(),
        NULL_SECURITY_DOMAIN,
        resource.toURI(),
        stripPath(resource, path),
        validator);
  }

  private URI stripPath(URL resource, String path) throws URISyntaxException {
    return new URI(resource.toString().replace(path, ""));
  }

  private String toPath(String moduleId) {
    return CharMatcher.is('/').trimLeadingFrom(moduleId);
  }

  @Override
  public ModuleSource loadSource(URI uri, URI baseUri, Object validator) throws IOException, URISyntaxException {
    throw new UnsupportedOperationException();
  }
}
