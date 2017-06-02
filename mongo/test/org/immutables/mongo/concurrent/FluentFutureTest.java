/*
   Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.mongo.concurrent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class FluentFutureTest implements Function<Integer, String> {
  AtomicInteger calledTimes = new AtomicInteger();

  @Override
  public String apply(Integer input) {
    calledTimes.incrementAndGet();
    return Integer.toString(input);
  }

  @Test
  public void transform() {
    FluentFuture<String> future =
        FluentFutures.from(Futures.immediateFuture(1))
            .transform(this);

    check(calledTimes.get()).is(1);
    check(future.getUnchecked()).is("1");
  }

  @Test
  public void lazyTransform() {
    FluentFuture<String> future =
        FluentFutures.from(Futures.immediateFuture(1))
            .lazyTransform(this);

    check(calledTimes.get()).is(0);
    check(future.getUnchecked()).is("1");
    check(calledTimes.get()).is(1);
    check(future.getUnchecked()).is("1");
    check(calledTimes.get()).is(2);
  }
}
