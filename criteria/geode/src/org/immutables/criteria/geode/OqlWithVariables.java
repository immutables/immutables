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

package org.immutables.criteria.geode;

import java.util.List;
import java.util.Objects;

/**
 * <a href="https://geode.apache.org/docs/guide/16/developing/querying_basics/query_basics.html">OQL</a>
 * with bind variables.
 */
class OqlWithVariables {

  private final List<Object> variables;

  private final String oql;

  OqlWithVariables(List<Object> variables, String oql) {
    this.variables = Objects.requireNonNull(variables, "variables");
    this.oql = Objects.requireNonNull(oql, "oql");
  }

  List<Object> variables() {
    return variables;
  }

  String oql() {
    return oql;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("oql=[").append(oql).append("] ");
    if (!variables.isEmpty()) {
      builder.append(" with ").append(variables.size()).append(" variables [");
      for (int i = 0; i < variables.size(); i++) {
        builder.append("$").append(i + 1).append("=").append(variables.get(i));
        builder.append(", ");
      }
      builder.append("]");
    }

    return builder.toString();
  }
}
