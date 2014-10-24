/*
    Copyright 2014 Ievgen Lukash

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

import org.immutables.common.eventually.CompletedFutureModule;
import org.immutables.common.eventually.EventualProvidersModule;
import org.immutables.common.eventually.EventuallyProvides;
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
