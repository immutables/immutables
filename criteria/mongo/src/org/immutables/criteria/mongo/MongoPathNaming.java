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

package org.immutables.criteria.mongo;

import org.immutables.criteria.backend.ExpressionNaming;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Path;

import java.util.Objects;

/**
 * Aware of {@code _id} field name for primary key
 */
class MongoPathNaming implements PathNaming {

  private final Path idProperty;
  private final PathNaming delegate;

  MongoPathNaming(Path idProperty, PathNaming delegate) {
    this.idProperty = Objects.requireNonNull(idProperty, "idProperty");
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public String name(Path path) {
    return idProperty.equals(path) ? "_id" : delegate.name(path);
  }

  ExpressionNaming toExpression() {
    return expression -> MongoPathNaming.this.name((Path) expression);
  }
}
