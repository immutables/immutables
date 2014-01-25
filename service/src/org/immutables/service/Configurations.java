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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.annotations.Beta;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.inject.Module;
import javax.inject.Provider;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.marshal.internal.MarshalingSupport;

@Beta
public final class Configurations {
  private Configurations() {}

  private static final JsonFactory JS_JSON_CONFIG_FACTORY = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_COMMENTS)
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

  private static final TypeToken<Provider<Module>> PROVIDER_MODULE_TYPE =
      new TypeToken<Provider<Module>>() {};

  public static Module loadModule(String classpathUri, String configurationClassName) {
    return moduleFrom(unmarshalConfiguration(
        classpathUri, loadMarshaler(configurationClassName)));
  }

  private static Marshaler<?> loadMarshaler(String configurationClassName) {
    try {
      return MarshalingSupport.loadMarshalerFor(
          Configurations.class.getClassLoader().loadClass(configurationClassName));

    } catch (Exception ex) {
      throw new RuntimeException(
          String.format("Cannot load marshaler for %s", configurationClassName), ex);
    }
  }

  @SuppressWarnings("unchecked")
  private static Module moduleFrom(Object unmarshaledConfiguration) {
    // safe unchecked due to TypeToken#isAssignableFrom check
    TypeToken<?> typeOfConfiguration = TypeToken.of(unmarshaledConfiguration.getClass());
    if (PROVIDER_MODULE_TYPE.isAssignableFrom(typeOfConfiguration)) {
      return ((Provider<Module>) unmarshaledConfiguration).get();
    }

    throw new RuntimeException(
        String.format("Configuration object of type %s should be instance of %s",
            unmarshaledConfiguration.getClass(),
            PROVIDER_MODULE_TYPE));
  }

  private static Object unmarshalConfiguration(String classpathUri, Marshaler<?> marshaler) {
    try (JsonParser parser = JS_JSON_CONFIG_FACTORY.createParser(Resources.getResource(classpathUri))) {
      parser.nextToken();
      return marshaler.unmarshalInstance(parser);
    } catch (Exception e) {
      throw new RuntimeException(String.format("Cannot read resource [%s] using %s", classpathUri, marshaler), e);
    }
  }
}
