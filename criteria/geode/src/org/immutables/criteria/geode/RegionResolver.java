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

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.immutables.criteria.backend.ContainerNaming;
import org.immutables.criteria.backend.ContainerResolver;

import java.util.Objects;

public interface RegionResolver extends ContainerResolver<Region<?, ?>> {

  /**
   * Resolve region using default {@link ContainerNaming#DEFAULT} naming convention.
   * @see org.apache.geode.cache.RegionService#getRegion(String)
   */
  static RegionResolver defaultResolver(GemFireCache cache) {
    Objects.requireNonNull(cache, "cache");
    return ctx -> {
      String name = ContainerNaming.DEFAULT.name(ctx);
      Region<Object, Object> region = cache.getRegion(name);
      if (region == null) {
        throw new IllegalArgumentException(String.format("Failed to find geode region for %s. " +
                "Region %s not found in %s cache", ctx.getName(), name, cache.getName()));
      }
      return region;
    };
  }

}
