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
package org.immutables.service;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorState;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.jersey.internal.inject.Injections;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

class JerseyInjectBridges {
  private JerseyInjectBridges() {}

  private static final ThreadLocal<Set<Object>> skipInjectionInstancesTransfer = new ThreadLocal<>();
  private static final ThreadLocal<Injector> currentBridgeInjector = new ThreadLocal<>();

  static void installBridgingServiceLocatorGenerator() {
    try {
      Field generatorField = getAccessibleGeneratorField();
      ServiceLocatorGenerator generator = (ServiceLocatorGenerator) generatorField.get(null);
      LocatorGenerator wrappedLocatorGenerator = new LocatorGenerator(generator);
      generatorField.set(null, wrappedLocatorGenerator);
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  private static Field getAccessibleGeneratorField() throws Exception {
    Field generatorField = Injections.class.getDeclaredField("generator");
    generatorField.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(generatorField, generatorField.getModifiers() & ~Modifier.FINAL);
    return generatorField;
  }

  static void bridgeInjector(Injector injector) {
    currentBridgeInjector.set(injector);
  }

  static void skipInjectionForInstances(Set<Object> instances) {
    skipInjectionInstancesTransfer.set(instances);
  }

  private final static class LocatorGenerator implements ServiceLocatorGenerator {
    private final ServiceLocatorGenerator delegate;

    LocatorGenerator(ServiceLocatorGenerator delegate) {
      this.delegate = delegate;
    }

    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
      if (parent instanceof WrapperLocator) {
        parent = ((WrapperLocator) parent).delegate;
      }
      return new WrapperLocator(delegate.create(name, parent));
    }
  }

  /**
   * Skip instance injection for instances that supplied in instance set.
   * Bridges to thread local Guice injector if available
   */
  private final static class WrapperLocator implements ServiceLocator {

    final ServiceLocator delegate;
    private final Set<Object> instances;
    private boolean neutralContextClassLoader;

    public WrapperLocator(ServiceLocator delegate) {
      this.delegate = delegate;
      this.instances = getSkipInjectionInstances();
      initBridgeInjectorIfAvailable(delegate);
    }

    private static Set<Object> getSkipInjectionInstances() {
      return Objects.firstNonNull(skipInjectionInstancesTransfer.get(), ImmutableSet.of());
    }

    private static void initBridgeInjectorIfAvailable(ServiceLocator serviceLocator) {
      @Nullable
      Injector injector = currentBridgeInjector.get();
      if (injector != null) {
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(injector);
      }
    }

    /**
     * Overriden to skip injection for supplied instances. {@inheritDoc}
     */
    @Override
    public void inject(Object injectMe) {
      if (!instances.contains(injectMe)) {
        delegate.inject(injectMe);
      }
    }

    /**
     * Overriden to skip injection for supplied instances. {@inheritDoc}
     */
    @Override
    public void inject(Object injectMe, String strategy) {
      if (!instances.contains(injectMe)) {
        delegate.inject(injectMe, strategy);
      }
    }

    @Override
    public <T> T getService(Class<T> contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getService(contractOrImpl, qualifiers);
    }

    public <T> T getService(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getService(contractOrImpl, qualifiers);
    }

    public <T> T getService(Class<T> contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
      return delegate.getService(contractOrImpl, name, qualifiers);
    }

    public <T> T getService(Type contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
      return delegate.getService(contractOrImpl, name, qualifiers);
    }

    public <T> List<T> getAllServices(Class<T> contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getAllServices(contractOrImpl, qualifiers);
    }

    public <T> List<T> getAllServices(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getAllServices(contractOrImpl, qualifiers);
    }

    public <T> List<T> getAllServices(Annotation qualifier, Annotation... qualifiers) throws MultiException {
      return delegate.getAllServices(qualifier, qualifiers);
    }

    public List<?> getAllServices(Filter searchCriteria) throws MultiException {
      return delegate.getAllServices(searchCriteria);
    }

    public <T> ServiceHandle<T> getServiceHandle(Class<T> contractOrImpl, Annotation... qualifiers)
        throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, qualifiers);
    }

    public <T> ServiceHandle<T> getServiceHandle(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, qualifiers);
    }

    public <T> ServiceHandle<T> getServiceHandle(Class<T> contractOrImpl, String name, Annotation... qualifiers)
        throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, name, qualifiers);
    }

    public <T> ServiceHandle<T> getServiceHandle(Type contractOrImpl, String name, Annotation... qualifiers)
        throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, name, qualifiers);
    }

    public <T> List<ServiceHandle<T>> getAllServiceHandles(Class<T> contractOrImpl, Annotation... qualifiers)
        throws MultiException {
      return delegate.getAllServiceHandles(contractOrImpl, qualifiers);
    }

    public List<ServiceHandle<?>> getAllServiceHandles(Type contractOrImpl, Annotation... qualifiers)
        throws MultiException {
      return delegate.getAllServiceHandles(contractOrImpl, qualifiers);
    }

    public List<ServiceHandle<?>> getAllServiceHandles(Annotation qualifier, Annotation... qualifiers)
        throws MultiException {
      return delegate.getAllServiceHandles(qualifier, qualifiers);
    }

    public List<ServiceHandle<?>> getAllServiceHandles(Filter searchCriteria) throws MultiException {
      return delegate.getAllServiceHandles(searchCriteria);
    }

    public List<ActiveDescriptor<?>> getDescriptors(Filter filter) {
      return delegate.getDescriptors(filter);
    }

    public ActiveDescriptor<?> getBestDescriptor(Filter filter) {
      return delegate.getBestDescriptor(filter);
    }

    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor, Injectee injectee) throws MultiException {
      return delegate.reifyDescriptor(descriptor, injectee);
    }

    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor) throws MultiException {
      return delegate.reifyDescriptor(descriptor);
    }

    public ActiveDescriptor<?> getInjecteeDescriptor(Injectee injectee) throws MultiException {
      return delegate.getInjecteeDescriptor(injectee);
    }

    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor, Injectee injectee)
        throws MultiException {
      return delegate.getServiceHandle(activeDescriptor, injectee);
    }

    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor) throws MultiException {
      return delegate.getServiceHandle(activeDescriptor);
    }

    @Deprecated
    public <T> T getService(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> root) throws MultiException {
      return delegate.getService(activeDescriptor, root);
    }

    public <T> T getService(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> root, Injectee injectee)
        throws MultiException {
      return delegate.getService(activeDescriptor, root, injectee);
    }

    public String getDefaultClassAnalyzerName() {
      return delegate.getDefaultClassAnalyzerName();
    }

    public void setDefaultClassAnalyzerName(String defaultClassAnalyzer) {
      delegate.setDefaultClassAnalyzerName(defaultClassAnalyzer);
    }

    public String getName() {
      return delegate.getName();
    }

    public long getLocatorId() {
      return delegate.getLocatorId();
    }

    public ServiceLocator getParent() {
      return delegate.getParent();
    }

    public void shutdown() {
      delegate.shutdown();
    }

    public ServiceLocatorState getState() {
      return delegate.getState();
    }

    public <T> T create(Class<T> createMe) {
      return delegate.create(createMe);
    }

    public <T> T create(Class<T> createMe, String strategy) {
      return delegate.create(createMe, strategy);
    }

    public void postConstruct(Object postConstructMe) {
      delegate.postConstruct(postConstructMe);
    }

    public void postConstruct(Object postConstructMe, String strategy) {
      delegate.postConstruct(postConstructMe, strategy);
    }

    public void preDestroy(Object preDestroyMe) {
      delegate.preDestroy(preDestroyMe);
    }

    public void preDestroy(Object preDestroyMe, String strategy) {
      delegate.preDestroy(preDestroyMe, strategy);
    }

    public <U> U createAndInitialize(Class<U> createMe) {
      return delegate.createAndInitialize(createMe);
    }

    public <U> U createAndInitialize(Class<U> createMe, String strategy) {
      return delegate.createAndInitialize(createMe, strategy);
    }

    @Override
    public boolean getNeutralContextClassLoader() {
      return neutralContextClassLoader;
    }

    @Override
    public void setNeutralContextClassLoader(boolean neutralContextClassLoader) {
      this.neutralContextClassLoader = neutralContextClassLoader;
    }
  }
}
