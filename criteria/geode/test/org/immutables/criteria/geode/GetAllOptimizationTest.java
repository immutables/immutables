/*
 * Copyright 2020 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.geode;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.WithSessionCallback;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.immutables.check.Checkers.check;

/**
 * Check that geode backend uses {@link Region#getAll(Collection)} when query
 * contains only expression on {@code ID} attribute like {@code id = 1 or id in [1, 2, 3]}
 *
 * It is considered more efficient than querying Geode directly (using OQL)
 * 
 * @see Region#getAll(Collection)
 */
@ExtendWith(GeodeExtension.class)
class GetAllOptimizationTest {

  private final PersonCriteria person = PersonCriteria.person;

  private final Cache cache;

  // intercepted calls
  private final List<MethodCall> calls;

  GetAllOptimizationTest(Cache cache) {
    this.cache = cache;
    this.calls = new ArrayList<>();
  }

  /**
   * With ID resolver {@link Region#getAll(Collection)} should be called
   */
  @SuppressWarnings("unchecked")
  @Test
  void optimization_getAll() {
    PersonRepository repository = createRepository(builder -> builder);
    repository.find(person.id.is("id1")).fetch();
    // expect some calls to be intercepted
    if (calls.isEmpty()) {
      Assertions.fail("Region.getAll(...) was not called. Check that this optimization is enabled");
    }
    check(calls).hasSize(1);
    check(calls.get(0).method.getName()).isIn("get", "getAll");
    check((Iterable<Object>) calls.get(0).args[0]).hasAll("id1");
    calls.clear();

    repository.find(person.id.in("id1", "id2")).fetch();
    check(calls).hasSize(1);
    check(calls.get(0).method.getName()).is("getAll");
    check((Iterable<Object>) calls.get(0).args[0]).hasAll("id1", "id2");
    calls.clear();

    // negatives should not use getAll
    repository.find(person.id.isNot("id1")).fetch();
    check(calls.isEmpty());

    repository.find(person.id.notIn("id1", "id2")).fetch();
    check(calls.isEmpty());

    // any other composite expression should not trigger getAll
    repository.find(person.id.is("id1").age.is(1)).fetch();
    repository.find(person.age.is(1).id.in(Arrays.asList("id1", "id2"))).fetch();
    check(calls.isEmpty());
  }

  /**
   * {@link Region#getAll(Collection)} should not be called without
   * known key metadata
   */
  @Test
  void withoutIdResolver() {
    // custom extractor based on generic function
    KeyExtractor.Factory extractor = KeyExtractor.generic(obj -> {
      Person person = (Person) obj;
      return person.id();
    });

    PersonRepository repository = createRepository(builder -> builder.keyExtractorFactory(extractor));
    repository.find(person.id.is("id1")).fetch();
    check(calls).isEmpty();
    repository.find(person.id.in("id1", "id2")).fetch();
    check(calls).isEmpty();
    // negatives
    repository.find(person.id.isNot("id1")).fetch();
    check(calls.isEmpty());
    repository.find(person.id.notIn("id1", "id2")).fetch();
    check(calls.isEmpty());
  }

  /**
   * Create repository using lambda for customization
   */
  private PersonRepository createRepository(Function<GeodeSetup.Builder, GeodeSetup.Builder> fn) {
    RegionResolver resolver = new LocalResolver(RegionResolver.defaultResolver(cache));
    GeodeSetup.Builder setup = fn.apply(GeodeSetup.builder().regionResolver(resolver));
    AutocreateRegion autocreate = new AutocreateRegion(cache);
    Backend backend = WithSessionCallback.wrap(new GeodeBackend(setup.build()), autocreate);
    return new PersonRepository(backend);
  }

  /**
   * Holder for Method and args
   */
  private static class MethodCall {
    private final Method method;
    private final Object[] args;

    private MethodCall(Method method, Object[] args) {
      this.method = method;
      this.args = args;
    }

    @Override
    public String toString() {
      return "MethodCall{" +
              "method=" + method +
              ", args=" + Arrays.toString(args) +
              '}';
    }
  }

  /**
   * Simple dynamic proxy to intercept method calls and arguments
   */
  private class MethodInterceptor implements InvocationHandler {

    private final Object delegate;

    private MethodInterceptor(Object delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      if (Arrays.asList("getAll", "get", "query").contains(name)) {
        calls.add(new MethodCall(method, args));
      }
      return method.invoke(delegate, args);
    }
  }

  /**
   * Decorates region with {@link MethodInterceptor}
   */
  private class LocalResolver implements RegionResolver {

    private final RegionResolver delegate;

    private LocalResolver(RegionResolver delegate) {
      this.delegate = delegate;
    }

    @Override
    public Region<?, ?> resolve(Class<?> entityType) {
      Region<?, ?> region = delegate.resolve(entityType);
      return (Region<?, ?>) Proxy.newProxyInstance(getClass().getClassLoader(),
              new Class[] {Region.class},
              new MethodInterceptor(region));
    }
  }

}
