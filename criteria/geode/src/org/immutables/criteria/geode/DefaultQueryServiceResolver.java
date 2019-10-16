/*
 * Copyright 2019 Immutables Authors and Contributors
 * Copyright 2010-2019 the Spring Data original author or authors.
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
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.internal.cache.LocalRegion;

import java.util.Objects;

/**
 * QueryService resolution logic copied from <a href="https://spring.io/projects/spring-data-geode">spring geode-data</a> project.
 *
 * <p>Specifically <a href="https://github.com/spring-projects/spring-data-geode/blob/9912c646f0edf52bc5b740d6f6b5b79e2f2ba172/src/main/java/org/springframework/data/gemfire/GemfireTemplate.java#L402">GemfireTemplate</a>
 *
 * @author Costin Leau
 * @author John Blum
 */
class DefaultQueryServiceResolver implements QueryServiceResolver {

  static final QueryServiceResolver INSTANCE = new DefaultQueryServiceResolver();

  private DefaultQueryServiceResolver() {}

  /**
   * Returns the {@link QueryService} used by this template in its query/finder methods.
   *
   * @param region {@link Region} used to acquire the {@link QueryService}.
   * @return the {@link QueryService} that will perform the query.
   * @see Region
   * @see Region#getRegionService()
   * @see org.apache.geode.cache.RegionService#getQueryService()
   * @see org.apache.geode.cache.client.ClientCache#getLocalQueryService()
   */
  @Override
  public QueryService resolve(Region<?, ?> region) {
    Objects.requireNonNull(region, "region");
    return region.getRegionService() instanceof ClientCache
            ? resolveClientQueryService(region)
            : queryServiceFrom(region);
  }

  private QueryService resolveClientQueryService(Region<?, ?> region) {
    Preconditions.checkArgument(region.getRegionService() instanceof ClientCache, "Expected to get %s got %s for region %s", ClientCache.class, region.getRegionService(), region.getFullPath());
    ClientCache clientCache = (ClientCache) region.getRegionService();

    return requiresLocalQueryService(region) ? clientCache.getLocalQueryService()
            : (requiresPooledQueryService(region) ? clientCache.getQueryService(poolNameFrom(region))
            : queryServiceFrom(region));
  }

  private boolean requiresLocalQueryService(Region<?, ?> region) {
    return Scope.LOCAL.equals(region.getAttributes().getScope()) && isLocalWithNoServerProxy(region);
  }

  private boolean isLocalWithNoServerProxy(Region<?, ?> region) {
    return region instanceof LocalRegion && !((LocalRegion) region).hasServerProxy();
  }

  private boolean requiresPooledQueryService(Region<?, ?> region) {
    String pool = poolNameFrom(region);
    return pool != null && !pool.isEmpty();
  }

  private QueryService queryServiceFrom(Region<?, ?> region) {
    return region.getRegionService().getQueryService();
  }

  private String poolNameFrom(Region<?, ?> region) {
    return region.getAttributes().getPoolName();
  }
}
