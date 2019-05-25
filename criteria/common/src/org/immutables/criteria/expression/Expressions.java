package org.immutables.criteria.expression;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.immutables.criteria.constraints.CriteriaContext;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A set of predefined utilities and factories for expressions like {@link Constant} or {@link Call}
 */
public final class Expressions {

  private Expressions() {}

  public static Path path(final String path) {
    return Path.of(path);
  }

  public static Constant constant(final Object value) {
    return Constant.of(value);
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
   * Hacky (and temporary) reflection until we define proper sub-classes for criterias
   * (to hide Expressional implementation).
   */
  public static Expression extract(Object object) {
    Objects.requireNonNull(object, "object");
    try {
      Class<?> current = object.getClass();
      while(current.getSuperclass() != null){
        if (Arrays.stream(current.getDeclaredFields()).anyMatch(f -> f.getName().equals("context"))) {
          Field field = current.getDeclaredField("context");
          field.setAccessible(true);
          CriteriaContext<?> context = (CriteriaContext<?>) field.get(object);
          return context.expression();
        }
        current = current.getSuperclass();
      }
    } catch (NoSuchFieldException|IllegalAccessException e) {
      throw new RuntimeException("No field in " + object.getClass().getName(), e);
    }

    throw new UnsupportedOperationException("No field context found in " + object.getClass().getName());
  }

  /**
   * Converts a {@link ExpressionVisitor} into a {@link ExpressionBiVisitor} (with ignored payload).
   */
  static <V> ExpressionBiVisitor<V, Void> toBiVisitor(ExpressionVisitor<V> visitor) {
    return new ExpressionBiVisitor<V, Void>() {
      @Override
      public V visit(Call call, @Nullable Void context) {
        return visitor.visit(call);
      }

      @Override
      public V visit(Constant constant, @Nullable Void context) {
        return visitor.visit(constant);
      }

      @Override
      public V visit(Path path, @Nullable Void context) {
        return visitor.visit(path);
      }
    };
  }

  /**
   * Combines expressions <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">Disjunctive normal form</a>
   */
  public static Expression dnf(Operator operator, Expression existing, Expression newExpression) {
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

  public static Call not(Call call) {
    return Expressions.call(Operators.NOT, call);
  }

  public static Call call(final Operator operator, Expression ... operands) {
    return call(operator, ImmutableList.copyOf(operands));
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
      public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
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
