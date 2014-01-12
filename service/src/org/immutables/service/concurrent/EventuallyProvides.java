package org.immutables.service.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Provides;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Async analog to Guice's {@literal @}{@link Provides}. Used to annotate
 * asynchronous provider methods used to describe transformation of async values.
 * Such methods may return values of type {@code V} or {@link ListenableFuture
 * ListenableFuture&lt;V&gt;} and binding target type will be
 * registered as {@link ListenableFuture ListenableFuture&lt;V&gt;}. Methods will be injected with
 * parameters, these parameters will contain unwrapped values ({@code P}), obtained when
 * all corresponding injectable ListenableFuture&lt;P&gt; is completed with values.
 * 
 * <pre>
 * {@literal @}EventuallyProvides
 * C async(A a, B b) {
 *   return new C(a.value(), b.getProperty());
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
 *      new Function&ltList&ltObject&gt;, C&gt;() {
 *        public C apply(List&ltObject&gt; input) {
 *          A a = input.get(0);
 *          B b = input.get(1);
 *          return new B(a.value(), b.getProperty());
 *        }
 *      });
 * }
 * </pre>
 * <p>
 * Defining class should be instantiable and injectable by Guice, provider methods should not be
 * static, private or abstract.
 * <p>
 * Advantage of eventual providers methods that one could include concise mapping methods to
 * describe sophisticated asynchronous transformations and derivations without much boilerplate.
 * <p>
 * Provider methods will inherit scope annotation from declaring class.
 * @see EventualProvidersModule
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Beta
public @interface EventuallyProvides {}
