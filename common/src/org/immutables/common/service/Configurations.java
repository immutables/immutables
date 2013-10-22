package org.immutables.common.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.inject.Module;
import javax.inject.Provider;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.marshal.internal.MarshalingSupport;

final class Configurations {
  private Configurations() {}

  private static final JsonFactory JS_JSON_CONFIG_FACTORY = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_COMMENTS)
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

  private static final TypeToken<Provider<Module>> PROVIDER_MODULE_TYPE =
      new TypeToken<Provider<Module>>() {};

  static Module loadModule(String classpathUri, String configurationClassName) {
    return extractModule(unmarshalConfiguration(
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
  private static Module extractModule(Object unmarshaledConfiguration) {
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
    try {
      JsonParser parser = JS_JSON_CONFIG_FACTORY.createParser(Resources.getResource(classpathUri));
      parser.nextToken();
      return marshaler.unmarshalInstance(parser);
    } catch (Exception e) {
      throw new RuntimeException(String.format("Cannot read resource [%s] using %s", classpathUri, marshaler), e);
    }
  }
}
