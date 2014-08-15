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

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Exposed;
import com.google.inject.Injector;
import com.google.inject.Provides;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Async analog to Guice's {@literal @}{@link Provides}. Used to annotate
 * asynchronous provider methods used to describe transformation of async values.
 * <p>
 * Such methods may return values of type {@code V} or {@link ListenableFuture
 * ListenableFuture&lt;V&gt;} and binding target type will be registered as {@link ListenableFuture
 * ListenableFuture&lt;V&gt;}. Methods will be injected with parameters, these parameters will
 * contain unwrapped values ({@code P}), obtained when all corresponding injectable
 * ListenableFuture&lt;P&gt; is completed with values. Binding will be private by default, use
 * {@literal @}{@link Exposed} annotation to make future binding injectable into other classes in
 * module. Provider method bindings will inherit scope annotation from declaring class if any.
 * Scoping is a serious concern which should be carefully crafted for particular use case. Most safe
 * way to run such group of asynchronous computations is to use {@literal @}Singleton scoped
 * providers running in a {@link Injector#createChildInjector(Iterable) child injector} created
 * specifically per computation.
 * 
 * <pre>
 * {@literal @}EventuallyProvides
 * C async(A a, B b) {
 * return new C(a.value(), b.getProperty());
 * }
 * </pre>
 * 
 * In this sample will create binding for {@code ListenableFuture<C>} that will be logically
 * equivalent to the following regular {@literal @}{@link Provides} method:
 * 
 * <pre>
 * {@literal @}Provides
 * ListenableFuture&lt;B&gt; async(ListenableFuture&lt;A&gt; a, ListenableFuture&lt;B&gt; b) {
 *   return Futures.transform(Futures.allAsList(a, b),
 *      new Function&lt;List&lt;Object&gt;, C&gt;() {
 *        public C apply(List&lt;Object&gt; input) {
 *          A a = input.get(0);
 *          B b = input.get(1);
 *          return new B(a.value(), b.getProperty());
 *        }
 *      });
 * }
 * </pre>
 * <p>
 * Advantage of eventual providers methods is that one could include number of concise mapping
 * methods to describe sophisticated asynchronous transformations and derivations without much
 * boilerplate.
 * <p>
 * Defining class should be instantiable and injectable by Guice, provider methods should not be
 * static, private or abstract.
 * <p>
 * @see EventualProvidersModule
 * @see Exposed
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Beta
public @interface EventuallyProvides {}
