/*
 * Copyright 2019 Immutables Authors and Contributors
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

package org.immutables.criteria.repository.rxjava2;

import java.util.Optional;

public interface RxJavaMapper1<T1> extends RxJavaFetcher<T1> {

  RxJavaFetcher<Optional<T1>> asOptional();

  interface DistinctLimitOffset<T> extends RxJavaMapper1<T>, RxJavaMapper1.LimitOffset<T> {
    RxJavaMapper1.LimitOffset<T> distinct();
  }

  interface LimitOffset<T> extends RxJavaMapper1.Offset<T> {
    RxJavaMapper1.Offset<T> limit(long limit);
  }

  interface Offset<T> extends RxJavaMapper1<T> {
    RxJavaMapper1<T> offset(long offset);
  }

}
