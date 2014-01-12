package org.immutables.service.concurrent;

import com.google.common.annotations.Beta;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import javax.inject.Qualifier;

/**
 * Qualifier annotation for {@link Executor} so it could be injected and for asynchronous
 * transformation dispatching.
 * @see EventualProvidersModule
 */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@Beta
public @interface Async {}
