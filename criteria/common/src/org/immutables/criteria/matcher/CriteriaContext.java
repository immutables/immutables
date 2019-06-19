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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.criteria.expression.DnfExpression;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Queryable;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Link between front-end (Criteria DSL) and <a href="https://cs.lmu.edu/~ray/notes/ir/">Intermediate Representation</a>
 * (internally known as {@link Expression}).
 */
public final class CriteriaContext implements Queryable {

  private final List<CriteriaCreator<?>> creators;
  private final DnfExpression expression;
  private final Path path;
  private final Operator operator;
  private final Class<?> entityClass;

  public CriteriaContext(Class<?> entityClass, CriteriaCreator<?> creator) {
    this(Operators.AND, entityClass, DnfExpression.create(Expressions.root(entityClass)), null, ImmutableList.of(creator));
  }

  private CriteriaContext(Operator operator, Class<?> entityClass, DnfExpression expression, Path path, List<CriteriaCreator<?>> creators) {
    this.creators = ImmutableList.copyOf( creators);
    this.expression = expression;
    this.path = path;
    this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    this.operator = operator;
  }

  public <S> CriteriaContext withCreators(CriteriaCreator<S> creator) {
    Objects.requireNonNull(creator, "creator");
    return new CriteriaContext(operator, entityClass, expression, path, ImmutableList.of(creator));
  }


  public <T1, T2> CriteriaContext withCreators(CriteriaCreator<T1> c1, CriteriaCreator<T2> c2) {
    Objects.requireNonNull(c1, "c1");
    Objects.requireNonNull(c2, "c2");

    return new CriteriaContext(operator, entityClass,  expression, path, ImmutableList.of(c1, c2));
  }

  public <T1, T2, T3> CriteriaContext withCreators(CriteriaCreator<T1> c1, CriteriaCreator<T2> c2, CriteriaCreator<T3> c3) {
    Objects.requireNonNull(c1, "c1");
    Objects.requireNonNull(c2, "c2");
    Objects.requireNonNull(c3, "c3");

    return new CriteriaContext(operator, entityClass, expression, path, ImmutableList.of(c1, c2, c3));
  }


  public <T1> CriteriaCreator.SingleFactory<T1> factory1() {
    Preconditions.checkState(creators.size() > 0, "Expected size > 0 got %s", creators.size());

    return new CriteriaCreator.SingleFactory<T1>() {
      @Override
      public CriteriaCreator<T1> creator1() {
        return (CriteriaCreator<T1>) creators.get(0);
      }

      @Override
      public CriteriaContext context() {
        return CriteriaContext.this;
      }
    };
  }

  public <T1, T2> CriteriaCreator.BiFactory<T1, T2> factory2() {
    Preconditions.checkState(creators.size() > 1, "Expected size > 1 got %s", creators.size());

    return new CriteriaCreator.BiFactory<T1, T2>() {
      @Override
      public CriteriaCreator<T2> creator2() {
        return (CriteriaCreator<T2>) creators.get(1);
      }

      @Override
      public CriteriaCreator<T1> creator1() {
        return (CriteriaCreator<T1>) creators.get(0);
      }

      @Override
      public CriteriaContext context() {
        return CriteriaContext.this;
      }
    };
  }

  public <T1, T2, T3> CriteriaCreator.TriFactory<T1, T2, T3>  factory3() {
    Preconditions.checkState(creators.size() > 2, "Expected size > 2 got %s", creators.size());

    return new CriteriaCreator.TriFactory<T1, T2, T3>() {
      @Override
      public CriteriaCreator<T3> creator3() {
        return (CriteriaCreator<T3>) creators.get(2);
      }

      @Override
      public CriteriaCreator<T2> creator2() {
        return (CriteriaCreator<T2>) creators.get(1);
      }

      @Override
      public CriteriaCreator<T1> creator1() {
        return (CriteriaCreator<T1>) creators.get(0);
      }

      @Override
      public CriteriaContext context() {
        return CriteriaContext.this;
      }
    };
  }

  /**
   *  adds an intermediate path
   */
  public CriteriaContext withPath(String pathAsString) {
    // clazz ==
    final Member member = Reflections.member(entityClass, pathAsString);
    final Path newPath = this.path != null ? this.path.with(member) : Path.of(member);
    return new CriteriaContext(operator, entityClass, expression, newPath, creators);
  }

  public CriteriaContext or() {
    if (operator == Operators.OR) {
      return this;
    }

    return new CriteriaContext(Operators.OR, entityClass, expression, path, creators);
  }

  @Override
  public Query query() {
    return this.expression.query();
  }

  public CriteriaContext withOperator(UnaryOperator<Expression> operator) {
    Objects.requireNonNull(operator, "operator");
    final Expression apply = operator.apply(path);
    final DnfExpression existing = expression;
    final DnfExpression newExpression = this.operator == Operators.AND ? existing.and(apply) : existing.or(apply);
    return new CriteriaContext(Operators.AND, entityClass, newExpression, null, creators);
  }

}
