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

public final class Operators {

  private Operators() {}

  public static final Operator EQUAL = new SimpleOperator("EQUAL", Boolean.class);

  public static final Operator NOT_EQUAL = new SimpleOperator("NOT_EQUAL", Boolean.class);

  public static final Operator IN = new SimpleOperator("IN", Boolean.class);

  public static final Operator NOT_IN = new SimpleOperator("NOT IN", Boolean.class);

  // boolean ops
  public static final Operator AND = new SimpleOperator("AND", Boolean.class);
  public static final Operator OR = new SimpleOperator("OR", Boolean.class);
  public static final Operator NOT = new SimpleOperator("NOT", Boolean.class);

  // vs is null / is not null ?
  public static final Operator IS_PRESENT = new SimpleOperator("IS_PRESENT", Boolean.class);
  public static final Operator IS_ABSENT = new SimpleOperator("IS_ABSENT", Boolean.class);

}
