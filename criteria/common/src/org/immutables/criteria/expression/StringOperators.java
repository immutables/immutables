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

public final class StringOperators {

  private StringOperators() {}

  /**
   * String regexp matching
   */
  public static final Operator MATCHES = new SimpleOperator("MATCHES", Boolean.class);
  /**
   * Check that string starts with a prefix
   */
  public static final Operator STARTS_WITH =  new SimpleOperator("STARTS_WITH", Boolean.class);
  /**
   * Check that string ends with a suffix
   */
  public static final Operator ENDS_WITH =  new SimpleOperator("ENDS_WITH", Boolean.class);


}
