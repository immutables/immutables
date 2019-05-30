package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

import java.util.function.UnaryOperator;

public interface CollectionMatcher<R, S, C, V> {

  default S all() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ALL, e);
    return Matchers.extract(this).<R, S, C>factory3().create2(expr);
  }

  default R all(UnaryOperator<C> consumer) {
    final CriteriaCreator.TriFactory<R, S, C> factory3 = Matchers.extract(this).<R, S, C>factory3();
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ALL,
            Matchers.toExpressionOperator(factory3::create3, consumer).apply(e));
    return factory3.create1(expr);
  }

  default S none() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NONE, e);
    return Matchers.extract(this).<R, S, C>factory3().create2(expr);
  }

  default R none(UnaryOperator<C> consumer) {
    final CriteriaCreator.TriFactory<R, S, C> factory3 = Matchers.extract(this).<R, S, C>factory3();
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NONE,
            Matchers.toExpressionOperator(factory3::create3, consumer).apply(e));
    return factory3.create1(expr);
  }

  default S any() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ANY, e);
    return Matchers.extract(this).<R, S, C>factory3().create2(expr);
  }

  default R any(UnaryOperator<C> consumer) {
    final CriteriaCreator.TriFactory<R, S, C> factory3 = Matchers.extract(this).<R, S, C>factory3();
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ANY,
            Matchers.toExpressionOperator(factory3::create3, consumer).apply(e));
    return factory3.create1(expr);
  }

  default S at(int index) {
    throw new UnsupportedOperationException();
  }

  default R contains(V value) {
    return Matchers.extract(this).<R, S, C>factory3().create1(e -> Expressions.call(Operators.CONTAINS, e));
  }

  default R isEmpty() {
    return Matchers.extract(this).<R, S, C>factory3().create1(e -> Expressions.call(Operators.EMPTY, e));
  }

  default R isNotEmpty() {
    return Matchers.extract(this).<R, S, C>factory3().create1(e -> Expressions.not(Expressions.call(Operators.EMPTY, e)));
  }

  default R hasSize(int size) {
    UnaryOperator<Expression> expr = e -> Expressions.call(Operators.SIZE, e, Expressions.constant(size));
    return Matchers.extract(this).<R, S, C>factory3().create1(expr);

  }


  interface Self<V> extends CollectionMatcher<Self, Self, Self, V> {}

}
