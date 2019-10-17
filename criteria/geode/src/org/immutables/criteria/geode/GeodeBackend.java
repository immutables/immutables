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

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.CqAttributesFactory;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.QueryService;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.IdExtractor;
import org.immutables.criteria.backend.IdResolver;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WatchEvent;
import org.immutables.criteria.expression.AggregationCall;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Backend for <a href="https://geode.apache.org/">Apache Geode</a>
 */
public class GeodeBackend implements Backend {

  static final Logger logger = Logger.getLogger(GeodeBackend.class.getName());

  private final GeodeSetup setup;


  public GeodeBackend(GeodeSetup setup) {
    this.setup = Objects.requireNonNull(setup, "setup");
  }

  @Override
  public Backend.Session open(Class<?> entityType) {
    Objects.requireNonNull(entityType, "context");
    return new Session(entityType, setup);
  }

  static class Session implements Backend.Session {

    final Class<?> entityType;
    final Region<Object, Object> region;
    final IdExtractor idExtractor;
    final IdResolver idResolver;
    final QueryService queryService;

    private Session(Class<?> entityType, GeodeSetup setup) {
      this.entityType = Objects.requireNonNull(entityType, "entityType");
      @SuppressWarnings("unchecked")
      Region<Object, Object> region = (Region<Object, Object>) setup.regionResolver().resolve(entityType);
      this.region = region;
      this.idResolver = setup.idResolver();
      this.idExtractor = IdExtractor.fromResolver(idResolver);
      this.queryService = setup.queryServiceResolver().resolve(region);
    }

    @Override
    public Class<?> entityType() {
      return entityType;
    }

    @Override
    public Result execute(Operation operation) {
      return DefaultResult.of(Flowable.defer(() -> executeInternal(operation)));
    }

    private Publisher<?> executeInternal(Operation operation) {
      if (operation instanceof StandardOperations.Select) {
        return Flowable.fromCallable(new SyncSelect(this, (StandardOperations.Select) operation)).flatMapIterable(x -> x);
      } else if (operation instanceof StandardOperations.Update) {
        return Flowable.fromCallable(new SyncUpdate(this, (StandardOperations.Update) operation));
      } else if (operation instanceof StandardOperations.Insert) {
        return Flowable.fromCallable(new SyncInsert(this, (StandardOperations.Insert) operation));
      } else if (operation instanceof StandardOperations.Delete) {
        return Flowable.fromCallable(new SyncDelete(this, (StandardOperations.Delete) operation));
      } else if (operation instanceof StandardOperations.Watch) {
        return watch((StandardOperations.Watch) operation);
      }

      return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported by %s",
              operation, GeodeBackend.class.getSimpleName())));
    }

    private <T> Publisher<WatchEvent<T>> watch(StandardOperations.Watch operation) {
      return Flowable.create(e -> {
        final FlowableEmitter<WatchEvent<T>> emitter = e.serialize();
        final String oql = toOql(operation.query(), false).oql();
        final CqAttributesFactory factory = new CqAttributesFactory();
        factory.addCqListener(new GeodeEventListener<>(oql, emitter));
        final CqQuery cqQuery = queryService.newCq(oql, factory.create());
        emitter.setDisposable(new CqDisposable(cqQuery));
        cqQuery.execute();
      }, BackpressureStrategy.ERROR);
    }

    OqlWithVariables toOql(Query query, boolean useBindVariables) {
      if (query.count() && (query.hasAggregations() || !query.groupBy().isEmpty())) {
        throw new UnsupportedOperationException("Aggregations / Group By and count(*) are not yet supported");
      }

      final StringBuilder oql = new StringBuilder("SELECT");
      if (!query.hasProjections()) {
        oql.append(query.count() ? " COUNT(*) " : " * ");
      } else {
        // explicitly add list of projections
        List<String> paths = query.projections().stream()
                .map(Session::toProjection)
                .collect(Collectors.toList());
        String projections = query.count() ? " COUNT(*) " : String.join(", ", paths);
        oql.append(" ");
        oql.append(projections);
        oql.append(" ");
      }

      oql.append(" FROM ").append(region.getFullPath());
      final List<Object> variables = new ArrayList<>();
      if (query.filter().isPresent()) {
        OqlWithVariables withVars = Geodes.converter(useBindVariables).convert(query.filter().get());
        oql.append(" WHERE ").append(withVars.oql());
        variables.addAll(withVars.variables());
      }

      if (!query.groupBy().isEmpty()) {
        oql.append(" GROUP BY ");
        oql.append(query.groupBy().stream().map(Session::toProjection).collect(Collectors.joining(", ")));
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

    private static String toProjection(Expression expression) {
      if (expression instanceof AggregationCall) {
        AggregationCall aggregation = (AggregationCall) expression;
        return String.format("%s(%s)", aggregation.operator().name(), toProjection(aggregation.arguments().get(0)));
      }

      return ((Path) expression).toStringPath();
    }
  }
}
