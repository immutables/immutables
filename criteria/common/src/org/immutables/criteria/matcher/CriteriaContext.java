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

package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Queryable;
import org.immutables.criteria.reflect.ClassScanner;

import java.lang.reflect.Member;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Link between front-end (Criteria DSL) and <a href="https://cs.lmu.edu/~ray/notes/ir/">Intermediate Representation</a>
 * (internally known as {@link Expression}). Keeps current state of the expression.
 */
public final class CriteriaContext implements Queryable {

  private final Expression expression;
  private final Path path;
  final Class<?> entityClass;
  private final CriteriaContext root;
  private final CriteriaContext parent;
  private final CriteriaCreator<?> creator;

  public CriteriaContext(Class<?> entityClass, CriteriaCreator<?> creator) {
    this(entityClass, new DnfExpression(), null, creator, null);
  }

  CriteriaContext(Class<?> entityClass, Expression expression, Path path, CriteriaCreator<?> creator, CriteriaContext parent) {
    this.expression = expression;
    this.path = path;
    this.creator = Objects.requireNonNull(creator, "creator");
    this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    this.parent = parent;
    this.root = parent != null ? parent.root() : this;
  }

  public Path path() {
    return path;
  }

  public Expression expression() {
    if (expression instanceof DnfExpression) {
      return dnfExpression().isEmpty() ? path() : dnfExpression().simplify();
    }

    if (expression == null) {
      return path();
    }

    return expression;
  }

  CriteriaContext root() {
    return root;
  }

  @SuppressWarnings("unchecked")
  <R> R create() {
    return (R) createWith(creator);
  }

  CriteriaCreator creator() {
    return creator;
  }

  /**
   * Create context as root but keep same expression
   */
  private CriteriaContext newRoot() {
    CriteriaContext rootContext = root();
    return new CriteriaContext(entityClass, expression, rootContext.path, rootContext.creator, null);
  }

  /**
   *  adds an intermediate path
   */
  public <T> CriteriaContext newChild(Class<?> type, String pathAsString, CriteriaCreator<T> creator) {
    // first look for fields then methods
    final Member member = Stream.concat(ClassScanner.of(type).skipMethods().stream(), ClassScanner.of(type).skipFields().stream())
            .filter(m -> m.getName().equals(pathAsString))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Path %s not found in %s", pathAsString, type)));

    final Path newPath = this.path != null ? this.path.append(member) : Path.ofMember(member);
    return new CriteriaContext(entityClass, expression, newPath, creator, this);
  }

  /**
   * Create nested context for lambdas used in {@link WithMatcher} or {@link NotMatcher}.
   * It is considered new root expression
   */
  <T1, T2> CriteriaContext nested() {
    return new CriteriaContext(entityClass, new DnfExpression(), path, creator, null);
  }

  public CriteriaContext or() {
    return new CriteriaContext(entityClass, dnfExpression().or(), path, creator, parent);
  }

  private DnfExpression dnfExpression() {
    return (DnfExpression) expression;
  }

  @Override
  public Query query() {
    final ImmutableQuery query = Query.of(entityClass);
    Expression expression = expression();
    if (expression instanceof DnfExpression) {
      DnfExpression dnfExpression = dnfExpression();
      return dnfExpression.isEmpty() ? query : query.withFilter(dnfExpression.simplify());
    } else if (expression != null) {
      return query.withFilter(expression);
    }

    return query;
  }

  <R> R createWith(CriteriaCreator<R> creator) {
    return creator.create(this);
  }

  CriteriaContext applyRaw(UnaryOperator<Expression> fn) {
    return new CriteriaContext(entityClass, fn.apply(path), path, creator, parent);
  }

  <R> R applyAndCreateRoot(UnaryOperator<Expression> fn) {
    return apply(fn).newRoot().create();
  }

  CriteriaContext apply(UnaryOperator<Expression> fn) {
    Objects.requireNonNull(fn, "fn");
    final Expression apply = fn.apply(path);
    final Expression newExpression;
    if (expression instanceof DnfExpression) {
      newExpression = dnfExpression().and(apply);
    } else if (expression != null) {
      newExpression = Expressions.and(expression, apply);
    } else {
      newExpression = apply;
    }
    final CriteriaContext parentOrSelf = parent != null ? parent : this;
    return new CriteriaContext(entityClass, newExpression, parentOrSelf.path, parentOrSelf.creator, parentOrSelf.parent);
  }
}
