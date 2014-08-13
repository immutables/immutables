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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import java.lang.annotation.Annotation;
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
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

class JerseyInjectBridges {
  private JerseyInjectBridges() {}

  private static final ThreadLocal<Set<Object>> skipInjectionInstancesTransfer = new ThreadLocal<>();
  private static final ThreadLocal<Injector> currentBridgeInjector = new ThreadLocal<>();

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

  static ServiceLocatorGenerator createGenerator(ServiceLocatorGenerator delegate) {
    return new LocatorGenerator(delegate);
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
     * Overridden to skip injection for supplied instances. {@inheritDoc}
     */
    @Override
    public void inject(Object injectMe) {
      if (!instances.contains(injectMe)) {
        delegate.inject(injectMe);
      }
    }

    /**
     * Overridden to skip injection for supplied instances. {@inheritDoc}
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

    @Override
    public <T> T getService(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getService(contractOrImpl, qualifiers);
    }

    @Override
    public <T> T getService(Class<T> contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
      return delegate.getService(contractOrImpl, name, qualifiers);
    }

    @Override
    public <T> T getService(Type contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
      return delegate.getService(contractOrImpl, name, qualifiers);
    }

    @Override
    public <T> List<T> getAllServices(Class<T> contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getAllServices(contractOrImpl, qualifiers);
    }

    @Override
    public <T> List<T> getAllServices(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getAllServices(contractOrImpl, qualifiers);
    }

    @Override
    public <T> List<T> getAllServices(Annotation qualifier, Annotation... qualifiers) throws MultiException {
      return delegate.getAllServices(qualifier, qualifiers);
    }

    @Override
    public List<?> getAllServices(Filter searchCriteria) throws MultiException {
      return delegate.getAllServices(searchCriteria);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Class<T> contractOrImpl, Annotation... qualifiers)
        throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, qualifiers);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, qualifiers);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Class<T> contractOrImpl, String name, Annotation... qualifiers)
        throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, name, qualifiers);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Type contractOrImpl, String name, Annotation... qualifiers)
        throws MultiException {
      return delegate.getServiceHandle(contractOrImpl, name, qualifiers);
    }

    @Override
    public <T> List<ServiceHandle<T>> getAllServiceHandles(Class<T> contractOrImpl, Annotation... qualifiers)
        throws MultiException {
      return delegate.getAllServiceHandles(contractOrImpl, qualifiers);
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Type contractOrImpl, Annotation... qualifiers)
        throws MultiException {
      return delegate.getAllServiceHandles(contractOrImpl, qualifiers);
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Annotation qualifier, Annotation... qualifiers)
        throws MultiException {
      return delegate.getAllServiceHandles(qualifier, qualifiers);
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Filter searchCriteria) throws MultiException {
      return delegate.getAllServiceHandles(searchCriteria);
    }

    @Override
    public List<ActiveDescriptor<?>> getDescriptors(Filter filter) {
      return delegate.getDescriptors(filter);
    }

    @Override
    public ActiveDescriptor<?> getBestDescriptor(Filter filter) {
      return delegate.getBestDescriptor(filter);
    }

    @Override
    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor, Injectee injectee) throws MultiException {
      return delegate.reifyDescriptor(descriptor, injectee);
    }

    @Override
    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor) throws MultiException {
      return delegate.reifyDescriptor(descriptor);
    }

    @Override
    public ActiveDescriptor<?> getInjecteeDescriptor(Injectee injectee) throws MultiException {
      return delegate.getInjecteeDescriptor(injectee);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor, Injectee injectee)
        throws MultiException {
      return delegate.getServiceHandle(activeDescriptor, injectee);
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor) throws MultiException {
      return delegate.getServiceHandle(activeDescriptor);
    }

    @Override
    @Deprecated
    public <T> T getService(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> root) throws MultiException {
      return delegate.getService(activeDescriptor, root);
    }

    @Override
    public <T> T getService(ActiveDescriptor<T> activeDescriptor, ServiceHandle<?> root, Injectee injectee)
        throws MultiException {
      return delegate.getService(activeDescriptor, root, injectee);
    }

    @Override
    public String getDefaultClassAnalyzerName() {
      return delegate.getDefaultClassAnalyzerName();
    }

    @Override
    public void setDefaultClassAnalyzerName(String defaultClassAnalyzer) {
      delegate.setDefaultClassAnalyzerName(defaultClassAnalyzer);
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public long getLocatorId() {
      return delegate.getLocatorId();
    }

    @Override
    public ServiceLocator getParent() {
      return delegate.getParent();
    }

    @Override
    public void shutdown() {
      delegate.shutdown();
    }

    @Override
    public ServiceLocatorState getState() {
      return delegate.getState();
    }

    @Override
    public <T> T create(Class<T> createMe) {
      return delegate.create(createMe);
    }

    @Override
    public <T> T create(Class<T> createMe, String strategy) {
      return delegate.create(createMe, strategy);
    }

    @Override
    public void postConstruct(Object postConstructMe) {
      delegate.postConstruct(postConstructMe);
    }

    @Override
    public void postConstruct(Object postConstructMe, String strategy) {
      delegate.postConstruct(postConstructMe, strategy);
    }

    @Override
    public void preDestroy(Object preDestroyMe) {
      delegate.preDestroy(preDestroyMe);
    }

    @Override
    public void preDestroy(Object preDestroyMe, String strategy) {
      delegate.preDestroy(preDestroyMe, strategy);
    }

    @Override
    public <U> U createAndInitialize(Class<U> createMe) {
      return delegate.createAndInitialize(createMe);
    }

    @Override
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
