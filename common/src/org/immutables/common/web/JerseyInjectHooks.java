package org.immutables.common.web;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
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

class JerseyInjectHooks {
  private JerseyInjectHooks() {}

  private static final ThreadLocal<Set<Object>> skipInjectionInstancesTransfer = new ThreadLocal<>();

  private static Field generatorField;

  static void installInjectionSkippingCapableServiceLocatorGenerator() {
    try {
      aquireAccessibleGeneratorField();
      ServiceLocatorGenerator generator = (ServiceLocatorGenerator) generatorField.get(null);
      generatorField.set(null, new LocatorGenerator(generator));
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  private static void aquireAccessibleGeneratorField() throws Exception {
    generatorField = Injections.class.getDeclaredField("generator");
    generatorField.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(generatorField, generatorField.getModifiers() & ~Modifier.FINAL);
  }

  static void skipInjectionForInstances(Set<Object> instances) {
    skipInjectionInstancesTransfer.set(instances);
  }

  private static Set<Object> getSkipInjectionInstances() {
    return Objects.firstNonNull(skipInjectionInstancesTransfer.get(), ImmutableSet.of());
  }

  private final static class LocatorGenerator implements ServiceLocatorGenerator {
    private final ServiceLocatorGenerator delegate;

    LocatorGenerator(ServiceLocatorGenerator delegate) {
      this.delegate = delegate;
    }

    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
      if (parent instanceof InjectionSkippingWrapperLocator) {
        parent = ((InjectionSkippingWrapperLocator) parent).delegate;
      }
      return new InjectionSkippingWrapperLocator(delegate.create(name, parent));
    }
  }

  /**
   * Skip instance injection for instances that supplied in instance set
   */
  private final static class InjectionSkippingWrapperLocator implements ServiceLocator {

    final ServiceLocator delegate;
    private final Set<Object> instances;

    public InjectionSkippingWrapperLocator(ServiceLocator delegate) {
      this.delegate = delegate;
      this.instances = getSkipInjectionInstances();
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
  }
}
