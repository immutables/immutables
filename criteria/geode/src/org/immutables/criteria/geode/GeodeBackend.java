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

import com.google.common.base.Preconditions;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Single;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.CqAttributesFactory;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.Struct;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.IdExtractor;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WatchEvent;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Backend for <a href="https://geode.apache.org/">Apache Geode</a>
 */
public class GeodeBackend implements Backend {

  private final RegionResolver resolver;

  /**
   * Convert Geode specific {@link QueryService#UNDEFINED} value to null
   */
  private final static Function<Object, Object> UNDEFINED_TO_NULL = value -> QueryService.UNDEFINED.equals(value) ? null : value;

  public GeodeBackend(GeodeSetup setup) {
    Objects.requireNonNull(setup, "setup");
    this.resolver = setup.regionResolver();
  }

  @Override
  public Backend.Session open(Class<?> context) {
    Objects.requireNonNull(context, "context");
    @SuppressWarnings("unchecked")
    Region<Object, Object> region = (Region<Object, Object>) resolver.resolve(context);
    return new Session(region);
  }

  @SuppressWarnings("unchecked")
  private static class Session implements Backend.Session {

    private final Region<Object, Object> region;

    private Session(Region<Object, Object> region) {
      this.region = Objects.requireNonNull(region, "region");
    }

    @Override
    public <T> Publisher<T> execute(Operation operation) {
      if (operation instanceof StandardOperations.Select) {
        return query((StandardOperations.Select<T>) operation);
      } else if (operation instanceof StandardOperations.Insert) {
        return (Publisher<T>) insert((StandardOperations.Insert) operation);
      } else if (operation instanceof StandardOperations.Delete) {
        return (Publisher<T>) delete((StandardOperations.Delete) operation);
      } else if (operation instanceof StandardOperations.Watch) {
        return (Publisher<T>) watch((StandardOperations.Watch<T>) operation);
      }


      return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported by %s",
              operation, GeodeBackend.class.getSimpleName())));
    }

    private <T> Flowable<T> query(StandardOperations.Select<T> op) {
      if (!op.query().groupBy().isEmpty()) {
        throw new UnsupportedOperationException("GroupBy not supported by " + GeodeBackend.class.getSimpleName());
      }

      // for projections use tuple function
      Function<Object, T> maybeTupleFn = op.query().projections().isEmpty() ? x -> (T) x : obj -> (T) toTuple(op.query(), obj);

      return Flowable.fromCallable(() -> {
        OqlWithVariables oql = toOql(op.query(), true);
        return (Iterable<Object>) region.getRegionService().getQueryService().newQuery(oql.oql()).execute(oql.variables().toArray(new Object[0]));
      })
        .flatMapIterable(x -> x)
        .map(maybeTupleFn::apply);
    }

    private static ProjectedTuple toTuple(Query query, Object result) {
      if (!(result instanceof Struct)) {
        Preconditions.checkArgument(query.projections().size() == 1, "Expected single projection got %s", query.projections().size());
        return ProjectedTuple.ofSingle(query.projections().get(0), UNDEFINED_TO_NULL.apply(result));
      }

      Struct struct = (Struct) result;
      // convert undefined to nulls
      List<?> values = Arrays.stream(struct.getFieldValues()).map(UNDEFINED_TO_NULL).collect(Collectors.toList());
      return ProjectedTuple.of(query.projections(), values);
    }

    private <T> Flowable<WriteResult> insert(StandardOperations.Insert<T> op) {
      if (op.values().isEmpty()) {
        return Flowable.just(WriteResult.empty());
      }

      // TODO cache id extractor
      final Function<T, Object> idExtractor = IdExtractor.reflection((Class<T>) op.values().get(0).getClass())::extract;
      final Map<Object, T> toInsert = op.values().stream().collect(Collectors.toMap(idExtractor, x -> x));
      final Region<Object, T> region = (Region<Object, T>) this.region;
      return Flowable.fromCallable(() -> {
        region.putAll(toInsert);
        return WriteResult.unknown();
      });
    }

    private <T> Flowable<WriteResult> delete(StandardOperations.Delete op) {
      if (!op.query().filter().isPresent()) {
        // no filter means delete all (ie clear whole region)
        return Completable.fromRunnable(region::clear)
                .toSingleDefault(WriteResult.unknown())
                .toFlowable();
      }

      final Expression filter = op.query().filter().orElseThrow(() -> new IllegalStateException("For " + op));
      final Optional<List<?>> ids = Geodes.canDeleteByKey(filter);
      // list of ids is present in the expression
      if (ids.isPresent()) {
        // delete by key: map.remove(key)
        return Completable.fromRunnable(() -> region.removeAll(ids.get()))
                .toSingleDefault(WriteResult.unknown())
                .toFlowable();
      }

      final GeodeQueryVisitor visitor = new GeodeQueryVisitor(true, path -> String.format("e.value.%s", path.toStringPath()));
      final OqlWithVariables oql = filter.accept(visitor);

      final String query = String.format("select distinct e.key from %s.entries e where %s", region.getFullPath(), oql.oql());

      return Single.fromCallable(() -> region.getRegionService()
              .getQueryService().newQuery(query).execute(oql.variables().toArray(new Object[0])))
              .flatMapCompletable(list -> Completable.fromRunnable(() -> region.removeAll((Collection<Object>) list)))
              .toSingleDefault(WriteResult.unknown())
              .toFlowable();
    }

    private <T> Publisher<WatchEvent<T>> watch(StandardOperations.Watch<T> operation) {
      return Flowable.create(e -> {
        final FlowableEmitter<WatchEvent<T>> emitter = e.serialize();
        final String oql = toOql(operation.query(), false).oql();
        final CqAttributesFactory factory = new CqAttributesFactory();
        factory.addCqListener(new GeodeEventListener<>(oql, emitter));
        final CqQuery cqQuery = region.getRegionService().getQueryService().newCq(oql, factory.create());
        emitter.setDisposable(new CqDisposable(cqQuery));
        cqQuery.execute();
      }, BackpressureStrategy.ERROR);
    }

    private OqlWithVariables toOql(Query query, boolean useBindVariables) {
      final StringBuilder oql = new StringBuilder("SELECT");
      if (query.projections().isEmpty()) {
        oql.append(" * ");
      } else {
        // explicitly add list of projections
        List<String> paths = query.projections().stream().map(p -> (Path) p)
                .map(Path::toStringPath)
                .collect(Collectors.toList());
        oql.append(" ");
        oql.append(String.join( ", ", paths));
        oql.append(" ");
      }

      oql.append(" FROM ").append(region.getFullPath());
      final List<Object> variables = new ArrayList<>();
      if (query.filter().isPresent()) {
        OqlWithVariables withVars = Geodes.converter(useBindVariables).convert(query.filter().get());
        oql.append(" WHERE ").append(withVars.oql());
        variables.addAll(withVars.variables());
      }

      if (!query.collations().isEmpty()) {
        oql.append(" ORDER BY ");
        final String orderBy = query.collations().stream()
                .map(c -> c.path().toStringPath() + (c.direction().isAscending() ? "" : " DESC"))
                .collect(Collectors.joining(", "));

        oql.append(orderBy);
      }

      query.limit().ifPresent(limit -> oql.append(" LIMIT ").append(limit));
      query.offset().ifPresent(offset -> oql.append(" OFFSET ").append(offset));
      return new OqlWithVariables(variables, oql.toString());
    }
  }
}
