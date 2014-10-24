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
package org.immutables.common.repository.internal;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import org.immutables.common.repository.RepositorySetup;
import org.immutables.common.time.TimeMeasure;

public final class RepositorySetupExecutors {
  private RepositorySetupExecutors() {}

  private static final int DEFAULT_THREAD_POOL_CORE_SIZE = 5;
  private static final int DEFAULT_THREAD_POOL_MAXIMUM_SIZE = 15;
  private static final TimeMeasure DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME = TimeMeasure.minutes(1);

  private static final ThreadFactory DEFAULT_THREAD_FACTORY =
      new ThreadFactoryBuilder()
          .setNameFormat(RepositorySetup.class.getPackage().getName() + "-%s")
          .setDaemon(true)
          .build();

  public static ListeningExecutorService newExecutor() {
    return MoreExecutors.listeningDecorator(
        MoreExecutors.getExitingExecutorService(
            new ThreadPoolExecutor(
                DEFAULT_THREAD_POOL_CORE_SIZE,
                DEFAULT_THREAD_POOL_MAXIMUM_SIZE,
                DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME.value(),
                DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME.unit(),
                new LinkedBlockingQueue<Runnable>(),
                DEFAULT_THREAD_FACTORY)));
  }
}
