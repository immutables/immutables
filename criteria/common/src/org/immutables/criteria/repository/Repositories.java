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

import com.google.common.base.Preconditions;
import org.immutables.criteria.expression.Query;

import java.util.Objects;

/**
 * Utilities for repositories
 */
public final class Repositories {
  private Repositories() {}

  /**
   * Extract current query from the reader
   */
  public static Query toQuery(Reader<?, ?> reader) {
    Objects.requireNonNull(reader, "reader");
    Preconditions.checkArgument(reader instanceof AbstractReader,
            "expected %s to be instance of %s", reader.getClass().getName(), AbstractReader.class.getName());

    return ((AbstractReader) reader).query();
  }

}
