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

/**
 * Visitor pattern for traversing a tree of expressions.
 *
 * Consider using {@link ExpressionBiVisitor} if you need to propagate some context / payload.
 *
 * @param <V> visitor return type
 */
public interface ExpressionVisitor<V> {

  V visit(Call call);

  V visit(Constant constant);

  V visit(Path path);

  V visit(Root root);

}
