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
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.value.Value;

import java.util.Objects;

/**
 * Setup configuration to connect to Geode cluster
 */
@Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
@Value.Immutable
public interface GeodeSetup {

  @Value.Parameter
  RegionResolver regionResolver();

  /**
   * Strategy to extract key(s) from an object.
   */
  @Value.Default
  default KeyExtractor.Factory keyExtractorFactory() {
    return KeyExtractor.defaultFactory();
  }

  @Value.Default
  default QueryServiceResolver queryServiceResolver() {
    return QueryServiceResolver.defaultResolver();
  }

  static GeodeSetup of(GemFireCache cache) {
    Objects.requireNonNull(cache, "cache");
    return of(RegionResolver.defaultResolver(cache));
  }

  static GeodeSetup of(RegionResolver resolver) {
    return ImmutableGeodeSetup.of(resolver);  }

  static Builder builder() {
    return new Builder();
  }

  class Builder extends ImmutableGeodeSetup.Builder {}

}
