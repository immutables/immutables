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

import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.Repository;
import org.immutables.value.Value;

import java.util.List;
import java.util.OptionalLong;

/**
 * List of default operations which can be executed on the backend
 */
public final class Operations {

  private Operations() {}

  /**
   * Query sent to a backend
   */
  @Value.Immutable
  public interface Query<T> extends Backend.Operation<T> {

    @Value.Parameter
    DocumentCriteria<?> criteria();

    @Value.Parameter
    Class<T> returnType();

    OptionalLong limit();

    OptionalLong offset();
  }

  /**
   * Insert list of "documents"
   */
  @Value.Immutable
  public interface Insert extends Backend.Operation<Repository.Success> {
    @Value.Parameter
    List<?> entities();
  }

  /**
   * Delete documents using some criteria
   */
  @Value.Immutable
  public interface Delete extends Backend.Operation<Repository.Success> {
    @Value.Parameter
    DocumentCriteria<?> criteria();

  }

  @Value.Immutable
  public interface Watch<T> extends Backend.Operation<T> {
    @Value.Parameter
    DocumentCriteria<?> criteria();

  }


}
