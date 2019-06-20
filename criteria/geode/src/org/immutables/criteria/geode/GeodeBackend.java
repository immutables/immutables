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
import io.reactivex.Single;
import org.apache.geode.cache.Region;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.WriteResult;
import org.immutables.criteria.adapter.Backend;
import org.immutables.criteria.adapter.Operations;
import org.immutables.criteria.adapter.UnknownWriteResult;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Backend for <a href="https://geode.apache.org/">Apache Geode</a>
 */
public class GeodeBackend implements Backend {

  private final Region<Object, Object> region;

  public GeodeBackend(Region<?, ?> region) {
    this.region = Objects.requireNonNull((Region<Object, Object>) region, "region is null");
  }

  @Override
  public <T> Publisher<T> execute(Operation<T> operation) {
    if (operation instanceof Operations.Select) {
      return query((Operations.Select<T>) operation);
    } else if (operation instanceof Operations.Insert) {
      return (Publisher<T>) insert((Operations.Insert) operation);
    } else if (operation instanceof Operations.Delete) {
      return (Publisher<T>) delete((Operations.Delete) operation);
    }

    return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported by %s",
            operation, GeodeBackend.class.getSimpleName())));
  }

  private <T> Flowable<T> query(Operations.Select<T> op) {
    final StringBuilder oql = new StringBuilder();
    final Query query = op.query();

    oql.append("SELECT * FROM ").append(region.getFullPath());

    query.filter().ifPresent(e -> oql.append(" WHERE ").append(Geodes.converter().convert(e)));

    op.query().limit().ifPresent(limit -> oql.append(" LIMIT ").append(limit));
    op.query().offset().ifPresent(offset -> oql.append(" OFFSET ").append(offset));

    return Flowable.<Collection<T>>fromCallable(() -> region.query(oql.toString()))
            .flatMapIterable(x -> x);
  }

  private <T> Flowable<WriteResult> insert(Operations.Insert<T> op) {
    if (!(op instanceof Operations.KeyedInsert)) {
      throw new UnsupportedOperationException(
              String.format("%s supports only %s. Did you define a key (@%s) on your domain class ?",
              GeodeBackend.class.getSimpleName(),
              Operations.KeyedInsert.class.getSimpleName(),
              Criteria.Id.class.getName()));
    }

    final Operations.KeyedInsert<?, T> insert = (Operations.KeyedInsert<?, T>) op;
    final Region<Object, T> region = (Region<Object, T>) this.region;
    return Completable.fromRunnable(() -> region.putAll(insert.toMap())).toFlowable();
  }

  private <T> Flowable<WriteResult> delete(Operations.Delete op) {
    if (!op.query().filter().isPresent()) {
      // no filter means delete all (ie clear whole region)
      return Completable.fromRunnable(region::clear)
              .toSingleDefault(UnknownWriteResult.INSTANCE)
              .toFlowable();
    }

    final Expression filter = op.query().filter().orElseThrow(() -> new IllegalStateException("For " + op));
    final Optional<List<?>> ids = Geodes.canDeleteByKey(filter);
    // list of ids is present in the expression
    if (ids.isPresent()) {
      // delete by key: map.remove(key)
      return Completable.fromRunnable(() -> region.removeAll(ids.get()))
              .toSingleDefault(UnknownWriteResult.INSTANCE)
              .toFlowable();
    }


    final String predicate = filter.accept(new GeodeQueryVisitor(path -> String.format("e.value.%s", path.toStringPath())));

    final String query = String.format("select distinct e.key from %s.entries e where %s", region.getFullPath(), predicate);

    return Single.fromCallable(() -> region.query(query))
            .flatMapCompletable(list -> Completable.fromRunnable(() -> region.removeAll((Collection<Object>) list)))
            .toSingleDefault(UnknownWriteResult.INSTANCE)
            .toFlowable();
  }


}
