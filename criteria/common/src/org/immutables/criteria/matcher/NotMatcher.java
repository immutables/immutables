package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

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
    final CriteriaCreator.TriFactory<R, ?, C> factory3 = Matchers.extract(this).<R, Object, C>factory3();
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NOT,
            Matchers.toExpressionOperator(factory3::create3, operator).apply(e));
    return factory3.create1(expr);
  }

}
