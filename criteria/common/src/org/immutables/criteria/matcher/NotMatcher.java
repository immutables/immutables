package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;

import java.util.function.UnaryOperator;

/**
 * Allows chaining {@code NOT} operator:
 * 
 * <pre>
 *   {@code
 *     crit.not(f -> f.names.hasSize(2))
 *   }
 * </pre>
 */
public interface NotMatcher<R, C> {

  default R not(UnaryOperator<C> operator) {
    final CriteriaContext context = Matchers.extract(this);
    final CriteriaCreator.TriFactory<R, ?, C> factory3 = context.<R, Object, C>factory3();
    final UnaryOperator<Expression> expr = e -> Expressions.not((Call) Matchers.toExpressionOperator3(context, operator).apply(e));
    return factory3.create1(expr);

  }

}
