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

package org.immutables.criteria.backend;

import org.immutables.criteria.backend.NamingStrategy;
import org.immutables.criteria.expression.Path;

/**
 * Strategy to name {@link Path}s (represent as String).
 *
 * @see org.immutables.criteria.Criteria.Id
 */
public interface PathNaming extends NamingStrategy<Path> {

  /**
   * Return name of a field (aka {@link Path}) like {@code a.b.c} or {@code _id}.
   * Should take in consideration {@code ID} naming properties of the backend.
   * @param path
   * @return path representation as string
   */
  @Override
  String name(Path path);
}
