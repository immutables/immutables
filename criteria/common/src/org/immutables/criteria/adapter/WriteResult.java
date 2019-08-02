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

package org.immutables.criteria.adapter;

import java.util.OptionalLong;

/**
 * Result of a <i>successful</i> write operation. It is up to back-end to provide
 * some, all or none of exposed statistics.
 */
public interface WriteResult {

  /**
   * Default instance used when backend doesn't return statistics about a write operation
   */
  WriteResult UNKNOWN = new UnknownWriteResult();

  /**
   * Number of records after insert operation
   *
   * @return number of inserted records. empty optional if unknown
   */
  OptionalLong insertedCount();


  /**
   * Number of records deleted after a write operation
   *
   * @return number of deleted records. empty optional if unknown or operation not supported.
   */
  OptionalLong deletedCount();

  /**
   * Number of records updated after a write operation
   *
   * @return number of deleted records. empty optional if unknown or operation not supported.
   */
  OptionalLong updatedCount();

}
