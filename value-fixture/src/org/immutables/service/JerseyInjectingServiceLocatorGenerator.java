/*
    Copyright 2013-2014 Immutables Authors and Contributors

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

import java.util.ServiceLoader;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;

/**
 * This class is loaded by HK2 injection framework via JDK standard extension mechanism (as
 * described in documentaion for {@link ServiceLoader}). We use this as a way to wrap service
 * locators in our injection hooks.
 */
public final class JerseyInjectingServiceLocatorGenerator implements ServiceLocatorGenerator {

  private static final ServiceLocatorGenerator GENERATOR =
      JerseyInjectBridges.createGenerator(new ServiceLocatorGeneratorImpl());

  @Override
  public ServiceLocator create(String name, ServiceLocator parent) {
    return GENERATOR.create(name, parent);
  }

}
