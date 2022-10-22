/*
 * Copyright 2022 Immutables Authors and Contributors
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

package org.immutables.criteria.backend;

import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;

class WriteResultsTest {
  @Test
  void empty() {
    WriteResult empty = WriteResult.empty();
    check(empty.insertedCount().getAsLong()).is(0L);
    check(empty.updatedCount().getAsLong()).is(0L);
    check(empty.deletedCount().getAsLong()).is(0L);
    check(empty.totalCount().getAsLong()).is(0L);
  }

  @Test
  void unknown() {
    ImmutableWriteResult unknown = WriteResult.unknown();
    check(!unknown.insertedCount().isPresent());
    check(!unknown.updatedCount().isPresent());
    check(!unknown.deletedCount().isPresent());
    check(!unknown.totalCount().isPresent());
    check(unknown.withUpdatedCount(1).updatedCount().getAsLong()).is(1L);
  }

  @Test
  void totalCount() {
    ImmutableWriteResult empty = WriteResult.empty();
    check(empty.totalCount().getAsLong()).is(0L);
    check(empty.withInsertedCount(1).totalCount().getAsLong()).is(1L);
    check(empty.withDeletedCount(1).withInsertedCount(1)
            .totalCount().getAsLong()).is(2L);
  }
}