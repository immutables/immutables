package org.immutables.criteria.constraints;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Link between front-end (Criteria DSL) and <a href="https://cs.lmu.edu/~ray/notes/ir/">Intermediate Representation</a>
 * (internally known as {@link Expression}).
 */
public final class CriteriaContext<R> implements Expressional<R> {

  private final CriteriaCreator<R> creator;
  private final DnfExpression expression;
  private final Path path;
  private final Operator operator;

  public CriteriaContext(CriteriaCreator<R> creator) {
    this(Operators.AND, DnfExpression.create(), null, creator);
  }

  private CriteriaContext(Operator operator, DnfExpression expression, Path path, CriteriaCreator<R> creator) {
    this.creator = creator;
    this.expression = expression;
    this.path = path;
    this.operator = operator;
  }

  public <S> CriteriaContext<S> withCreator(CriteriaCreator<S> creator) {
    Objects.requireNonNull(creator, "creator");
    return new CriteriaContext<S>(operator, expression, path, creator);
  }

  public R create() {
    return creator.create(this);
  }

  /**
   *  adds an intermediate step (list of paths usually)
   */
  public CriteriaContext<R> add(Path path) {
    final Path newPath = this.path != null ? Expressions.path(this.path.path() + "." + path.path()) : path;
    return new CriteriaContext<>(operator, expression, newPath, creator);
  }

  public CriteriaContext<R> or() {
    if (operator == Operators.OR) {
      return this;
    }

    return new CriteriaContext<>(Operators.OR, expression, path, creator);
  }

  @Override
  public Expression expression() {
    return this.expression.expression();
  }

  @SuppressWarnings("unchecked")
  public R create(UnaryOperator<Expression> operator) {
    final Expression apply = operator.apply(path);
    final DnfExpression existing = expression;
    final DnfExpression newExpression = this.operator == Operators.AND ? existing.and(apply) : existing.or(apply);
    return new CriteriaContext<R>(Operators.AND, newExpression, null, creator).create();
  }

}
