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

package org.immutables.criteria.expression;

public final class IterableOperators {

  // means collection  is empty
  public static final Operator EMPTY = new SimpleOperator("EMPTY", Boolean.class);

  // size of the collection
  public static final Operator SIZE = new SimpleOperator("SIZE", Long.class);

  // contains
  public static final Operator CONTAINS = new SimpleOperator("CONTAINS", Boolean.class);

  // all elements match
  public static final Operator ALL = new SimpleOperator("ALL", Boolean.class);

  // no elements match
  public static final Operator NONE = new SimpleOperator("NONE", Boolean.class);

  // some elements match (at least one)
  public static final Operator ANY = new SimpleOperator("ANY", Boolean.class);

  private IterableOperators() {}

}
