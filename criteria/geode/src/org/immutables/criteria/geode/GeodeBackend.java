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

package org.immutables.criteria.geode;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import org.apache.geode.cache.Region;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.Repository;
import org.immutables.criteria.adapter.Backend;
import org.immutables.criteria.adapter.Operations;
import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.Objects;

public class GeodeBackend implements Backend {

  private final Region<?, ?> region;

  public GeodeBackend(Region<?, ?> region) {
    this.region = Objects.requireNonNull(region, "region is null");
  }

  @Override
  public <T> Publisher<T> execute(Operation<T> operation) {
    if (operation instanceof Operations.Query) {
      return query((Operations.Query<T>) operation);
    } else if (operation instanceof Operations.Insert) {
      return (Publisher<T>) insert((Operations.Insert) operation);
    }

    return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
  }

  private <T> Flowable<T> query(Operations.Query<T> op) {
    final StringBuilder query = new StringBuilder();

    query.append("SELECT * FROM ").append(region.getFullPath());

    final String predicate = Geodes.converter().convert(Criterias.toExpression(op.criteria()));
    if (!predicate.isEmpty()) {
      query.append(" WHERE ").append(predicate);
    }

    op.limit().ifPresent(limit -> query.append(" LIMIT ").append(limit));
    op.offset().ifPresent(offset -> query.append(" OFFSET ").append(offset));

    return Flowable.<Collection<T>>fromCallable(() -> region.query(query.toString()))
            .flatMapIterable(x -> x);
  }

  private <T> Flowable<Repository.Success> insert(Operations.Insert<T> op) {
    if (!(op instanceof Operations.KeyedInsert)) {
      throw new UnsupportedOperationException(
              String.format("%s supports only %s. Did you define a key (@%s) on your domain class ?",
              GeodeBackend.class.getSimpleName(),
              Operations.KeyedInsert.class.getSimpleName(),
              Criteria.Id.class.getName()));
    }


    Operations.KeyedInsert<?, T> insert = (Operations.KeyedInsert<?, T>) op;
    Region<Object, T> region = (Region<Object, T>) this.region;
    return Completable.fromRunnable(() -> region.putAll(insert.toMap())).toFlowable();
  }

}
