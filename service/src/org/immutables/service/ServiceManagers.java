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

import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Set;

@Beta
public final class ServiceManagers {
  private ServiceManagers() {}

  private static final Key<Set<Service>> SERVICE_SET_KEY = Key.get(new TypeLiteral<Set<Service>>() {});

  public static ServiceManager forServiceSet(Injector injector) {
    return new ServiceManager(injector.getInstance(SERVICE_SET_KEY));
  }

  public static ServiceManager forAllServices(Injector injector) {
    return new ServiceManager(getAllServices(injector));
  }

  public static Set<Service> getAllServices(Injector injector) {
    Set<Service> serviceInstances = Sets.newHashSet();
    for (Key<?> key : injector.getBindings().keySet()) {
      if (Service.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
        serviceInstances.add((Service) injector.getInstance(key));
      }
    }
    return serviceInstances;
  }
}
