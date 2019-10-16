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

import org.immutables.criteria.repository.MapperFunction4;
import org.immutables.criteria.repository.Tuple;

import java.util.function.Function;

public interface RxJavaMapper4<T1, T2, T3, T4> {

  <R> RxJavaFetcher<R> map(MapperFunction4<T1, T2, T3, T4, R> mapFn);

  <R> RxJavaFetcher<R> map(Function<? super Tuple, ? extends R> mapFn);

  interface DistinctLimitOffset<T1, T2, T3, T4> extends LimitOffset<T1, T2, T3, T4>, RxJavaMapper4<T1, T2, T3, T4> {
    LimitOffset<T1, T2, T3, T4> distinct();
  }

  interface LimitOffset<T1, T2, T3, T4> extends Offset<T1, T2, T3, T4> {
    Offset<T1, T2, T3, T4> limit(long limit);
  }

  interface Offset<T1, T2, T3, T4> extends RxJavaMapper4<T1, T2, T3, T4> {
    RxJavaMapper4<T1, T2, T3, T4> offset(long offset);
  }

}
