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

package org.immutables.criteria;

import com.google.common.base.Preconditions;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Queryable;

import java.util.Objects;

public final class Criterias {

  private Criterias() {}

  /**
   * Extracts {@link Query} from a criteria. Any criteria implements
   * {@code Queryable} interface at runtime.
   */
  public static Query toQuery(DocumentCriteria<?> criteria) {
    Objects.requireNonNull(criteria, "criteria");
    Preconditions.checkArgument(criteria instanceof Queryable, "%s should implement %s",
            criteria.getClass().getName(), Queryable.class.getName());

    return ((Queryable) criteria).query();
  }

  /**
   * Extract directly filter from a criteria
   */
  public static Expression toFilterExpression(DocumentCriteria<?> criteria) {
    return toQuery(criteria).filter().orElseThrow(() -> new IllegalArgumentException("no defined filter for " + criteria));
  }

}
