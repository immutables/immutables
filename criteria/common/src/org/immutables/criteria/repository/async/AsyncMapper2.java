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

package org.immutables.criteria.repository.async;

import org.immutables.criteria.repository.Tuple;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface AsyncMapper2<T1, T2> {

  <R> AsyncFetcher<R> map(BiFunction<T1, T2, R> mapFn);

  <R> AsyncFetcher<R> map(Function<? super Tuple, ? extends R> mapFn);

  interface DistinctLimitOffset<T1, T2> extends LimitOffset<T1, T2>, AsyncMapper2<T1, T2> {
    AsyncMapper2.LimitOffset<T1, T2> distinct();
  }

  interface LimitOffset<T1, T2> extends Offset<T1, T2> {
    AsyncMapper2.Offset<T1, T2> limit(long limit);
  }

  interface Offset<T1, T2> extends AsyncMapper2<T1, T2> {
    AsyncMapper2<T1, T2> offset(long offset);
  }

}
