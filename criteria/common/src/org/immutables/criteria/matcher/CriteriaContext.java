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

import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.IterableOperators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Queryable;
import org.immutables.criteria.expression.Visitors;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Expression holder
 *
 * Internally keeps two expressions {@code current} and {@code partial} which
 * are combined using different functions depending on current context.
 */
public final class CriteriaContext implements Queryable {

  /**
   * Singleton which stores class metadata for faster lookups
   */
  private static final MemberLookupCache MEMBER_LOOKUP_SINGLETON = new MemberLookupCache();

  private final CriteriaContext previous;
  private final ImmutableState state;
  private final MemberLookupCache memberLookup;

  public CriteriaContext(Class<?> entityType, CriteriaCreator<?> creator) {
    this(null,
            ImmutableState.builder()
                    .combiner(Combiner.and())
                    .creator(creator)
                    .entityType(entityType)
                    .build()
    );
  }

  CriteriaContext(CriteriaContext previous, ImmutableState state) {
    this.previous = previous;
    this.state = Objects.requireNonNull(state, "state");
    this.memberLookup = MEMBER_LOOKUP_SINGLETON;
  }

  @Value.Immutable
  interface State {
    @Nullable Expression current();
    @Nullable Expression partial();
    Combiner combiner();
    CriteriaCreator<?> creator();
    Class<?> entityType();

    @Value.Default
    @Nullable
    default Expression defaultPartial() {
      return null;
    }
  }

  public Path path() {
    return Visitors.toPath(state.partial());
  }

  ImmutableState state() {
    return state;
  }

  public Expression expression() {
    if (state.current() != null) {
      return state.current();
    }

    return state.partial();
  }

  private CriteriaContext first() {
    return previous != null ? previous.first() : this;
  }

  @SuppressWarnings("unchecked")
  <R> R create() {
    return (R) createWith(state.creator());
  }

  @SuppressWarnings("unchecked")
  <R> CriteriaCreator<R> creator() {
    return (CriteriaCreator<R>) state.creator();
  }

  /**
   *  adds an intermediate path to partial expression
   */
  public <T> CriteriaContext appendPath(Class<?> type, String pathAsString, CriteriaCreator<T> creator) {
    final Member member = memberLookup.find(type, pathAsString)
            .orElseThrow(() -> new IllegalArgumentException(String.format("Path %s not found in %s", pathAsString, type)));

    Path newPath;
    Expression partial = state.partial();
    if (partial == null) {
      newPath = Path.ofMember(member);
    } else if (partial instanceof Path) {
      newPath = Visitors.toPath(partial).append(member);
    } else {
      throw new IllegalStateException("Partial expression is not a path: " + partial);
    }

    return new CriteriaContext(this, state.withPartial(newPath).withCreator(creator));
  }

  /**
   * Create nested context for lambdas used in {@link WithMatcher} or {@link NotMatcher}.
   * It is considered new root expression
   */
  CriteriaContext nested() {
    // return new CriteriaContext(entityClass, new DnfExpression(), path, creator, null);
    ImmutableState newState = state.withCurrent(null).withDefaultPartial(state.partial()).withCombiner(Combiner.dnfAnd());
    return new CriteriaContext(null, newState);
  }

  public CriteriaContext or() {
    return new CriteriaContext(previous, state.withCombiner(Combiner.or()));
  }

  /**
   * Build {@link IterableMatcher#any()} sub-matcher.
   */
  public CriteriaContext any() {
    Path path = path();
    Class<?> newType; // new path starts at this root
    try {
      newType = (Class<?>) Matchers.iterableTypeArgument(path.returnType());
    } catch (IllegalArgumentException|ClassCastException e) {
      throw new IllegalArgumentException(String.format("At path %s for %s",
              path.toStringPath(), path.returnType()), e);
    }

    Path newPath = Path.ofClass(newType);
    Combiner combiner = ((left, right) -> Expressions.binaryCall(IterableOperators.ANY, path, right));
    return new CriteriaContext(previous, state.withCombiner(combiner).withPartial(newPath));
  }

  @Override
  public Query query() {
    ImmutableQuery query = Query.of(state.entityType());
    if (state.current() != null) {
      query = query.withFilter(state.current());
    }
    return query;
  }

  <R> R createWith(CriteriaCreator<R> creator) {
    return creator.create(this);
  }

  /**
   * Used by Aggregator Matchers (sum / count / avg etc.)
   */
  CriteriaContext applyRaw(UnaryOperator<Expression> fn) {
    return new CriteriaContext(previous, state.withPartial(fn.apply(state.partial())));
  }

  <R> R applyAndCreateRoot(UnaryOperator<Expression> fn, Combiner nextCombiner) {
    Expression newPartial = fn.apply(state.partial());
    Expression newExpression = state.combiner().combine(state.current(), newPartial);
    newPartial = newExpression;
    CriteriaContext context = this.previous;

    while (context != null) {
      Expression tmpExpression = context.state.combiner().combine(context.state.current(), newPartial);
      if (isPathExtention(tmpExpression, newExpression)) {
        newExpression = tmpExpression;
        newPartial = tmpExpression;
      }

      context = context.previous;
    }
    
    // use initial creator
    CriteriaCreator<?> creator = first().state.creator();

    ImmutableState newState = state.withCombiner(nextCombiner)
            .withCreator(creator)
            .withCurrent(newExpression)
            .withPartial(state.defaultPartial());

    return new CriteriaContext(null, newState).create();
  }

  <R> R applyAndCreateRoot(UnaryOperator<Expression> fn) {
    return applyAndCreateRoot(fn, Combiner.dnfAnd());
  }

  private static boolean isPathExtention(Expression newExpression, Expression expression) {
    if (newExpression instanceof Call && expression instanceof Call) {
      return !((Call) newExpression).arguments().get(0).equals(((Call) expression).arguments().get(0));
    } else {
      return true;
    }
  }
}
