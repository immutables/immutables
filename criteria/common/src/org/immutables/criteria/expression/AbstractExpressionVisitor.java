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

package org.immutables.criteria.expression;

import java.util.Objects;
import java.util.function.Function;

/**
 * Future-proof visitor (in terms of API evolution).
 * Clients are encouraged to use this API instead of raw interface ({@link ExpressionVisitor}).
 */
public class AbstractExpressionVisitor<T> implements ExpressionVisitor<T> {

  private final Function<? super Expression, T> mapper;

  protected AbstractExpressionVisitor(Function<? super Expression, T> mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  protected AbstractExpressionVisitor(T value) {
    this(e -> value);
  }

  @Override
  public T visit(Call call) {
    return mapper.apply(call);
  }

  @Override
  public T visit(Constant constant) {
    return mapper.apply(constant);
  }

  @Override
  public T visit(Path path) {
    return mapper.apply(path);
  }

  @Override
  public T visit(Query query) {
    return query.expression().map(e -> e.accept(this)).orElseGet(() -> mapper.apply(query));
  }
}
