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

package org.immutables.criteria.backend;

import java.lang.reflect.Member;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caches ID resolution
 */
class CachedIdResolver implements IdResolver {

  private final ConcurrentMap<Class<?>, Member> cache;
  private final IdResolver delegate;

  private CachedIdResolver(IdResolver delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.cache = new ConcurrentHashMap<>();
  }

  @Override
  public Member resolve(Class<?> type) {
    return cache.computeIfAbsent(type, delegate::resolve);
  }

  public static IdResolver of(IdResolver resolver) {
    return new CachedIdResolver(resolver);
  }
}
