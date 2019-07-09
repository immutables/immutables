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
import org.immutables.criteria.expression.Expression;
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
  private final Class<?> entityClass;
  private final CriteriaContext parent;

  public CriteriaContext(Class<?> entityClass, CriteriaCreator<?> creator) {
    this(entityClass, new DnfExpression(), null, ImmutableList.of(creator), null);
  }

  private CriteriaContext(Class<?> entityClass, DnfExpression expression, Path path, List<CriteriaCreator<?>> creators, CriteriaContext parent) {
    this.creators = ImmutableList.copyOf( creators);
    this.expression = expression;
    this.path = path;
    this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    this.parent = parent;
  }

  public <T1, T2> CriteriaContext withCreators(CriteriaCreator<T1> c1, CriteriaCreator<T2> c2) {
    Objects.requireNonNull(c1, "c1");
    Objects.requireNonNull(c2, "c2");

    // keep root unchanged here (it should change only for nested matchers)
    final CriteriaCreator<?> root = creators.get(0);
    return new CriteriaContext(entityClass, expression, path, ImmutableList.of(root, c2), parent);
  }

  public <T1, T2> CriteriaCreator.Factory<T1, T2> factory() {
    Preconditions.checkState(creators.size() == 2, "Expected size == 2 got %s", creators.size());

    return new CriteriaCreator.Factory<T1, T2>() {
      @Override
      public CriteriaCreator<T2> nested() {
        return (CriteriaCreator<T2>) creators.get(1);
      }

      @Override
      public CriteriaCreator<T1> root() {
        return (CriteriaCreator<T1>) creators.get(0);
      }

      @Override
      public CriteriaContext context() {
        return CriteriaContext.this;
      }
    };
  }


  public <T1, T2> CriteriaContext newChild() {
    return new CriteriaContext(entityClass, new DnfExpression(), path, ImmutableList.of(factory().nested(), creators.get(1)), this);
  }


  public CriteriaContext ofParent() {
    Preconditions.checkState(parent != null, "parent is null");
    final DnfExpression newExpression = parent.expression.and(expression);
    return new CriteriaContext(entityClass, newExpression, path.parent().orElse(null), creators, parent.parent);
  }


  private CriteriaContext withPath(Path newPath) {
    return new CriteriaContext(entityClass, expression, newPath, creators, parent);
  }

  /**
   *  adds an intermediate path
   */
  public CriteriaContext withPath(Class<?> type, String pathAsString) {
    // clazz ==
    final Member member = Reflections.member(type, pathAsString);
    final Path newPath = this.path != null ? this.path.with(member) : Path.of(member);
    return withPath(newPath);
  }


  public CriteriaContext or() {
    return new CriteriaContext(entityClass, expression.or(), path, creators, parent);
  }

  @Override
  public Query query() {
    final Query query = Query.of(entityClass);
    return !expression.isEmpty() ? query.withFilter(expression.simplify()) : query;
  }

  public CriteriaContext apply(UnaryOperator<Expression> fn) {
    Objects.requireNonNull(fn, "fn");
    final Expression apply = fn.apply(path);
    final DnfExpression newExpression = expression.and(apply);
    return new CriteriaContext(entityClass, newExpression, parent != null ? parent.path : null, creators, parent);
  }

}
