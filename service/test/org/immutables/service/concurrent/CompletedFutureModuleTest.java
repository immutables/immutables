package org.immutables.service.concurrent;

import com.google.inject.Exposed;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.immutables.common.concurrent.FluentFuture;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class CompletedFutureModuleTest {
  @Test
  public void futuresDereferencing() {
    Injector injectorWithFutures = Guice.createInjector(EventualProvidersModule.from(SampleFutureProvider.class));
    FluentFuture<Module> futureModule = CompletedFutureModule.from(injectorWithFutures);
    Injector resultModule = Guice.createInjector(futureModule.getUnchecked());

    check(resultModule.getInstance(Integer.class)).is(1);
    check(resultModule.getInstance(String.class)).is("a");
  }

  static class SampleFutureProvider {
    @Exposed
    @EventuallyProvides
    Integer integer() {
      return 1;
    }

    @Exposed
    @EventuallyProvides
    String string() {
      return "a";
    }
  }
}
