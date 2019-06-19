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

import javax.annotation.Nullable;

/**
 * Visitor which also accepts a payload.
 *
 * @param <V> visitor return type
 * @param <C> context type
 */
public interface ExpressionBiVisitor<V, C> {

  V visit(Call call, @Nullable C context);

  V visit(Constant constant, @Nullable C context);

  V visit(Path path, @Nullable C context);

  V visit(Query root, @Nullable C context);

}
