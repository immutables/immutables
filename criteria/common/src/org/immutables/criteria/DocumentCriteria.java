/*
   Copyright 2013-2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.immutables.criteria;

import org.immutables.criteria.constraints.Call;
import org.immutables.criteria.constraints.Expression;
import org.immutables.criteria.constraints.ExpressionVisitor;
import org.immutables.criteria.constraints.Literal;
import org.immutables.criteria.constraints.Path;

import javax.annotation.Nullable;

/**
 * Base class of Criteria API. Right now used as a marker interface.
 * Generated code extends this class.
 *
 * @param <C> Criteria self-type, allowing {@code this}-returning methods to avoid needing subclassing
 * @param <T> type of the document being evaluated by this criteria
 */
public interface DocumentCriteria<C extends DocumentCriteria<C, T>, T> extends Expression<T> {

  /**
   * Expose expression used by this criteria
   */
  Expression<T> expression();

  @Nullable
  @Override
  default <R> R accept(ExpressionVisitor<R> visitor) {
    final Expression<T> expression = expression();

    if (expression instanceof Path) {
      return visitor.visit((Path<?>) expression);
    } else if (expression instanceof Call) {
      return visitor.visit((Call<?>) expression);
    } else if (expression instanceof Literal) {
      return visitor.visit((Literal<?>) expression);
    }

    throw new UnsupportedOperationException(String.format("Unknown expression type %s for %s",
            expression.getClass().getName(), getClass()));

  }
}
