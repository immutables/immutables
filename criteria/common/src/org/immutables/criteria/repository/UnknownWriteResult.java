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

package org.immutables.criteria.repository;

import java.util.OptionalLong;

/**
 * Used as a <b>null object</b> if backend can't provide information about a write operation
 */
final class UnknownWriteResult implements WriteResult {

  UnknownWriteResult() {}

  @Override
  public OptionalLong insertedCount() {
    return OptionalLong.empty();
  }

  @Override
  public OptionalLong deletedCount() {
    return OptionalLong.empty();
  }

  @Override
  public OptionalLong updatedCount() {
    return OptionalLong.empty();
  }
}
