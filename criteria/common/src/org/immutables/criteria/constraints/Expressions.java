package org.immutables.criteria.constraints;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * A set of predefined utilities and factories for expressions like {@link Literal} or {@link Call}
 */
public final class Expressions {

  private Expressions() {}

  public static Path path(final String path) {
    Preconditions.checkNotNull(path, "path");

    return new Path() {
      @Override
      public String path() {
        return path;
      }

      @Nullable
      @Override
      public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
        return visitor.visit(this, context);
      }
    };
  }

  public static Literal nullLiteral(Class<?> type) {
    return NullLiteral.ofType(type);
  }

  public static Literal literal(final Object value) {
    Preconditions.checkArgument(value != null, "Use nullLiteral() factory method for nulls");
    return new Literal() {
      @Override
      public Object value() {
        return value;
      }

      @Override
      public Type valueType() {
        return value.getClass();
      }

      @Nullable
      @Override
      public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
        return visitor.visit(this, context);
      }
    };
  }

  public static Expression and(Expression first, Expression second) {
    return and(Arrays.asList(first, second));
  }

  public static  Expression and(Iterable<? extends Expression> expressions) {
    return reduce(Operators.AND, expressions);
  }

  public static  Expression or(Expression first, Expression second) {
    return or(Arrays.asList(first ,second));
  }

  public static  Expression or(Iterable<? extends Expression> expressions) {
    return reduce(Operators.OR, expressions);
  }

  private static  Expression reduce(Operator operator, Iterable<? extends Expression> expressions) {
    final Iterable<? extends Expression> filtered = Iterables.filter(expressions, e -> !isNil(e) );
    final int size = Iterables.size(filtered);

    if (size == 0) {
      return nil();
    } else if (size == 1) {
      return filtered.iterator().next();
    }

    return call(operator, expressions);
  }

  /**
   * Combines expressions <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">Disjunctive normal form</a>
   */
  public static  Expression dnf(Operator operator, Expression existing, Expression newExpression) {
    if (!(operator == Operators.AND || operator == Operators.OR)) {
      throw new IllegalArgumentException(String.format("Expected %s for operator but got %s",
              Arrays.asList(Operators.AND, Operators.OR), operator));
    }

    if (isNil(existing)) {
      return DnfExpression.create().and(newExpression);
    }

    if (!(existing instanceof DnfExpression)) {
      throw new IllegalStateException(String.format("Expected existing expression to be %s but was %s",
              DnfExpression.class.getName(), existing.getClass().getName()));
    }

    @SuppressWarnings("unchecked")
    final DnfExpression conjunction = (DnfExpression) existing;
    return operator == Operators.AND ? conjunction.and(newExpression) : conjunction.or(newExpression);
  }

  public static  Call call(final Operator operator, Expression ... operands) {
    return call(operator, Arrays.asList(operands));
  }

  public static  Call call(final Operator operator, final Iterable<? extends Expression> operands) {
    final List<Expression> ops = ImmutableList.copyOf(operands);
    return new Call() {
      @Override
      public List<Expression> getArguments() {
        return ops;
      }

      @Override
      public Operator getOperator() {
        return operator;
      }

      @Nullable
      @Override
      public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
        return visitor.visit(this, context);
      }
    };
  }

  /**
   * Used as sentinel for {@code noop} expression.
   */
  @SuppressWarnings("unchecked")
  public static  Expression nil() {
    return NilExpression.INSTANCE;
  }

  public static boolean isNil(Expression expression) {
    return expression == NilExpression.INSTANCE;
  }

}
