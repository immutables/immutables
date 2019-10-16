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

package org.immutables.criteria.reactor;

import java.util.Optional;

public interface ReactorMapper1<T1> extends ReactorFetcher<T1> {

  ReactorFetcher<Optional<T1>> asOptional();

  interface DistinctLimitOffset<T> extends LimitOffset<T> {
    ReactorMapper1.LimitOffset<T> distinct();
  }

  interface LimitOffset<T> extends ReactorMapper1.Offset<T> {
    ReactorMapper1.Offset<T> limit(long limit);
  }

  interface Offset<T> extends ReactorMapper1<T> {
    ReactorMapper1<T> offset(long offset);
  }

}
