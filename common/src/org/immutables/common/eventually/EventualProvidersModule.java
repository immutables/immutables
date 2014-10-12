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
package org.immutables.common.eventually;

import com.google.common.base.Preconditions;
import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Binder;
import com.google.inject.Exposed;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.PrivateBinder;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.internal.Annotations;
import com.google.inject.internal.Errors;
import com.google.inject.internal.util.StackTraceElements;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.Message;
import com.google.inject.spi.ProviderWithDependencies;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.immutables.common.concurrent.FluentFutures;

/**
 * Create special mix-in module created from defining class with special asynchronous transformation
 * methods annotated with {@literal @}{@link EventuallyProvides}.
 * <p>
 * Basic example
 * 
 * <pre>
 * public class Providers {
 *   {@literal @}EventuallyProvides
 *   C combine(A a, B b) {
 *     return new C(a.value(), b.getProperty());
 *   }
 * 
 *   {@literal @}Exposed
 *   {@literal @}EventuallyProvides
 *   Z transformed(C c) {
 *     return c.transformed();
 *   }
 * }
 * </pre>
 * <p>
 * Having dependency on ListenableFuture&lt;A&gt; and ListenableFuture&lt;B&gt;, this module exposed
 * combined and transformed ListenableFuture&lt;Z&gt; available to injector.
 * <p>
 * <em>While super-classes could be used and will be scanned for such methods, method overriding is
 * not handled properly so avoid overriding provider methods. Use delegation to regular methods if some
 * functionality should be implemented or overridden.
 * </em>
 * <p>
 * To customize dispatching injector could provided with binding to {@literal @}
 * {@link EventuallyAsync} {@link Executor}
 * @see EventuallyProvides
 * @param <T> defining class type
 */
@Beta
public final class EventualProvidersModule<T> implements Module {
  private static TypeToken<ListenableFuture<?>> LISTENABLE_FUTURE = new TypeToken<ListenableFuture<?>>() {};
  private static Executor DEFAULT_EXECUTOR = MoreExecutors.directExecutor();

  private final Class<T> providersClass;
  private final TypeToken<T> type;

  private final ImmutableList<EventualProvider<?>> providers;
  @Nullable
  private final Class<? extends Annotation> scopeAnnotation;
  private final Errors errors;
  private final Object source;

  private EventualProvidersModule(Class<T> providersClass) {
    this.providersClass = providersClass;
    this.source = StackTraceElements.forType(providersClass);
    this.type = TypeToken.of(providersClass);
    this.errors = new Errors(source);
    this.scopeAnnotation = Annotations.findScopeAnnotation(errors, providersClass);
    this.providers = introspectProviders();
  }

  public static <T> EventualProvidersModule<T> from(Class<T> asyncProviderClass) {
    return new EventualProvidersModule<>(asyncProviderClass);
  }

  @Override
  public void configure(Binder binder) {
    binder = binder.withSource(source);

    if (errors.hasErrors()) {
      for (Message message : errors.getMessages()) {
        binder.addError(message);
      }
    } else {
      bindWithPrivateBinder(binder.newPrivateBinder());
    }
  }

  private void bindWithPrivateBinder(PrivateBinder privateBinder) {
    ScopedBindingBuilder scoper = privateBinder.bind(providersClass);

    if (scopeAnnotation != null) {
      scoper.in(scopeAnnotation);
    }

    for (EventualProvider<?> p : providers) {
      p.bindFutureProvider(privateBinder);
    }
  }

  private ImmutableList<EventualProvider<?>> introspectProviders() {
    ImmutableList.Builder<EventualProvider<?>> builder = ImmutableList.builder();

    // FIXME handle method overriding?
    for (Class<?> t : type.getTypes().classes().rawTypes()) {
      if (t != Object.class) {
        for (Method m : t.getDeclaredMethods()) {
          if (m.isAnnotationPresent(EventuallyProvides.class)) {
            builder.add(
                providerFor(
                    type.method(m),
                    StackTraceElements.forMember(m)));
          }
        }
      }
    }

    return builder.build();
  }

  private EventualProvider<?> providerFor(Invokable<T, ?> method, Object source) {
    Errors methodErrors = errors.withSource(source);

    Annotation[] annotations = method.getAnnotations();

    verifyMethodAccessibility(methodErrors, method, source);

    @Nullable
    Annotation bindingAnnotation =
        Annotations.findBindingAnnotation(methodErrors, method, annotations);

    verifyAbsenseOfScopeAnnotation(methodErrors, annotations, source);

    List<Dependency<ListenableFuture<?>>> dependencies =
        Lists.newArrayListWithCapacity(method.getParameters().size());

    for (Parameter parameter : method.getParameters()) {
      dependencies.add(extractDependency(methodErrors, parameter));
    }

    Key<ListenableFuture<?>> bindingKey = futureKey(method.getReturnType(), bindingAnnotation);
    boolean exposedBinding = method.isAnnotationPresent(Exposed.class);

    return new EventualProvider<>(
        method,
        exposedBinding,
        dependencies,
        bindingKey,
        scopeAnnotation,
        source);
  }

  private void verifyAbsenseOfScopeAnnotation(Errors methodErrors, Annotation[] annotations, Object source) {
    @Nullable
    Class<? extends Annotation> methodScopeAnnotation =
        Annotations.findScopeAnnotation(methodErrors, annotations);
    if (methodScopeAnnotation != null) {
      methodErrors.addMessage(
          "Misplaced scope annotation @%s on method @%s %s.%n    Scope annotation will be inherited from enclosing class %s",
          methodScopeAnnotation.getSimpleName(),
          EventuallyProvides.class.getSimpleName(),
          source,
          providersClass.getSimpleName());
    }
  }

  private void verifyMethodAccessibility(Errors methodErrors, Invokable<T, ?> method, Object source) {
    if (method.isStatic()
        || method.isPrivate()
        || method.isAbstract()
        || method.isSynthetic()) {
      methodErrors.addMessage(
          "Method @%s %s must not be private, static or abstract",
          EventuallyProvides.class.getSimpleName(),
          source);
    } else if (!method.isPublic()) {
      method.setAccessible(true);
    }
  }

  Dependency<ListenableFuture<?>> extractDependency(Errors methodErrors, Parameter parameter) {
    @Nullable
    Annotation bindingAnnotation =
        Annotations.findBindingAnnotation(
            methodErrors,
            parameter.getDeclaringInvokable(),
            parameter.getAnnotations());

    return Dependency.get(futureKey(
        parameter.getType(),
        bindingAnnotation));
  }

  Key<ListenableFuture<?>> futureKey(TypeToken<?> typeToken, @Nullable Annotation bindingAnnotation) {
    TypeLiteral<ListenableFuture<?>> futureType = futureTypeLiteralFrom(typeToken);
    return bindingAnnotation != null
        ? Key.get(futureType, bindingAnnotation)
        : Key.get(futureType);
  }

  // safe unchecked: wrapping and subtyping verifies that type will be ListenableFuture of some type
  @SuppressWarnings("unchecked")
  TypeLiteral<ListenableFuture<?>> futureTypeLiteralFrom(TypeToken<?> type) {
    return (TypeLiteral<ListenableFuture<?>>) TypeLiteral.get(
        (LISTENABLE_FUTURE.isAssignableFrom(type)
            ? type.getSubtype(ListenableFuture.class)
            : wrapAsListenableFuture(type)).getType());
  }

  <V> TypeToken<ListenableFuture<V>> wrapAsListenableFuture(TypeToken<V> valueType) {
    return new TypeToken<ListenableFuture<V>>() {}.where(new TypeParameter<V>() {}, valueType);
  }

  private enum UnwrapFutureProvider implements Function<Provider<ListenableFuture<?>>, ListenableFuture<?>> {
    FUNCTION;
    @Override
    public ListenableFuture<?> apply(Provider<ListenableFuture<?>> input) {
      return input.get();
    }
  }

  private class EventualProvider<V>
      implements ProviderWithDependencies<ListenableFuture<V>>,
      FutureFallback<V> {

    private final ImmutableList<Dependency<ListenableFuture<?>>> dependencies;
    private final ImmutableSet<Dependency<?>> dependencySet;
    private final Invokable<T, ?> method;
    private final boolean exposedBinding;
    private final Key<ListenableFuture<?>> bindingKey;
    private final Class<? extends Annotation> scopeAnnotation;
    private final Object source;

    private List<Provider<ListenableFuture<?>>> dependencyProviders;
    private Provider<T> targetInstanceProvider;

    EventualProvider(
        Invokable<T, ?> method,
        boolean exposedBinding,
        List<Dependency<ListenableFuture<?>>> dependencies,
        Key<ListenableFuture<?>> bindingKey,
        @Nullable Class<? extends Annotation> scopeAnnotation,
        Object source) {
      this.method = method;
      this.source = source;
      this.exposedBinding = exposedBinding;
      this.bindingKey = bindingKey;
      this.scopeAnnotation = scopeAnnotation;
      this.dependencies = ImmutableList.copyOf(dependencies);
      this.dependencySet = ImmutableSet.<Dependency<?>>builder()
          .addAll(dependencies)
          .add(Dependency.get(Key.get(Injector.class)))
          .add(Dependency.get(Key.get(type.getRawType())))
          .build();
    }

    @com.google.inject.Inject(optional = true)
    @EventuallyAsync
    Executor executor = DEFAULT_EXECUTOR;

    @Inject
    void init(Injector injector) {
      dependencyProviders = providersForDependencies(injector);
      targetInstanceProvider = injector.getProvider(providersClass);
    }

    private List<Provider<ListenableFuture<?>>> providersForDependencies(Injector injector) {
      List<Provider<ListenableFuture<?>>> providers = Lists.newArrayListWithCapacity(dependencies.size());
      for (Dependency<ListenableFuture<?>> d : dependencies) {
        providers.add(injector.getProvider(d.getKey()));
      }
      return providers;
    }

    void bindFutureProvider(PrivateBinder binder) {
      binder = binder.withSource(source);

      ScopedBindingBuilder scoper = binder.bind(bindingKey).toProvider(this);
      if (scopeAnnotation != null) {
        scoper.in(scopeAnnotation);
      }

      if (exposedBinding) {
        binder.expose(bindingKey);
      }
    }

    @Override
    public Set<Dependency<?>> getDependencies() {
      return dependencySet;
    }

    @Override
    public ListenableFuture<V> get() {
      return FluentFutures.from(
          Futures.allAsList(resolvedDependecies()))
          .withExecutor(executor)
          .transform(derivationFunction(targetInstanceProvider.get()))
          .withFallback(this);
    }

    private ImmutableList<ListenableFuture<?>> resolvedDependecies() {
      return FluentIterable.from(dependencyProviders)
          .transform(UnwrapFutureProvider.FUNCTION)
          .toList();
    }

    private AsyncFunction<List<Object>, V> derivationFunction(final T targetInstance) {
      return new AsyncFunction<List<Object>, V>() {
        // Safe unchecked: type checks was done during introspection
        @SuppressWarnings("unchecked")
        @Override
        public ListenableFuture<V> apply(List<Object> input) throws Exception {
          Object result = method.invoke(targetInstance, input.toArray());
          if (result == null) {
            Preconditions.checkNotNull(result, "Method @%s %s should not return null",
                EventuallyProvides.class.getSimpleName(),
                source);
          }
          if (result instanceof ListenableFuture<?>) {
            return (ListenableFuture<V>) result;
          }
          return Futures.immediateFuture((V) result);
        }
      };
    }

    @Override
    public ListenableFuture<V> create(Throwable t) throws Exception {
      if (t instanceof InvocationTargetException) {
        t = t.getCause();
      }
      t.setStackTrace(trimStackTrace(t.getStackTrace()));
      return Futures.immediateFailedFuture(t);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .addValue(source)
          .toString();
    }
  }

  private static StackTraceElement[] trimStackTrace(StackTraceElement[] stackTrace) {
    String[] trimmedPrefixes = {
        Futures.class.getPackage().getName(),
        Invokable.class.getPackage().getName(),
        EventualProvidersModule.class.getName()
    };
    List<StackTraceElement> list = Lists.newArrayListWithExpectedSize(stackTrace.length);
    stackLines: for (int i = 0; i < stackTrace.length; i++) {
      StackTraceElement element = stackTrace[i];
      for (int j = 0; j < trimmedPrefixes.length; j++) {
        String prefix = trimmedPrefixes[j];
        if (element.getClassName().startsWith(prefix)) {
          continue stackLines;
        }
      }
      list.add(element);
    }
    return list.toArray(new StackTraceElement[list.size()]);
  }
}
