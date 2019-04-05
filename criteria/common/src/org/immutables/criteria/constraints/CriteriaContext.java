package org.immutables.criteria.constraints;

import org.immutables.criteria.DocumentCriteria;

import java.util.function.UnaryOperator;

/**
 * Link between front-end (codegened Criteria) and back-end (built {@link Expression}).
 */
public final class CriteriaContext<R extends DocumentCriteria<R>> {

  private final CriteriaCreator<R> creator;
  private final DnfExpression<?> expression;
  private final Path<?> path;
  private final Operator operator;

  public CriteriaContext(CriteriaCreator<R> creator) {
    this(Operators.AND, DnfExpression.create(), null, creator);
  }

  private CriteriaContext(Operator operator, DnfExpression<?> expression, Path<?> path, CriteriaCreator<R> creator) {
    this.creator = creator;
    this.expression = expression;
    this.path = path;
    this.operator = operator;
  }

  public R create() {
    return creator.create(this);
  }

  /**
   *  adds an intermediate step (list of paths usually)
   */
  public CriteriaContext<R> add(Path<?> path) {
    final Path<?> newPath = this.path != null ? Expressions.path(this.path.path() + "." + path.path()) : path;
    return new CriteriaContext<>(operator, expression, newPath, creator);
  }

  public CriteriaContext<R> or() {
    if (operator == Operators.OR) {
      return this;
    }

    return new CriteriaContext<>(Operators.OR, expression, path, creator);
  }

  public Expression<?> expression() {
    return this.expression;
  }

  @SuppressWarnings("unchecked")
  public R create(UnaryOperator<Expression<?>> operator) {
    final Expression<Object> apply = (Expression<Object>) operator.apply(path);
    final DnfExpression<Object> existing = (DnfExpression<Object>) expression;
    final DnfExpression<?> newExpression = this.operator == Operators.AND ? existing.and(apply) : existing.or(apply);
    return new CriteriaContext<R>(Operators.AND, newExpression, null, creator).create();
  }

}
