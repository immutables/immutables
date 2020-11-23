/*
 * Copyright 2020 Immutables Authors and Contributors
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

package org.immutables.criteria.matcher;

import org.immutables.criteria.reflect.ClassScanner;
import org.immutables.value.Value;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Member;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caches reflection calls to speedup criteria creation.
 *
 * <p>Internal API not to be used outside criteria package</p>
 *
 * <p>Thread-safe implementation</p>
 */
@ThreadSafe
class MemberLookupCache {

  private final ConcurrentMap<Class<?>, ClassScanner> scanner;
  private final ConcurrentMap<ClassAndPath, Member> member;

  MemberLookupCache() {
    this.scanner = new ConcurrentHashMap<>();
    this.member = new ConcurrentHashMap<>();
  }

  Optional<Member> find(Class<?> type, String path) {
    ClassAndPath classAndPath = ImmutableClassAndPath.of(type, path);
    Member member = this.member.computeIfAbsent(classAndPath, key -> {
      ClassScanner scanner = this.scanner.computeIfAbsent(key.type(), MemberLookupCache::createScannerForType);
      return scanner.stream().filter(m -> m.getName().equals(path)).findFirst().orElse(null);
    });

    return Optional.ofNullable(member);
  }

  private static ClassScanner createScannerForType(Class<?> type) {
    // scan fields then methods. cache result
    return ClassScanner
            .concat(ClassScanner.onlyFields(type),
                    ClassScanner.onlyMethods(type))
            .cache(); // cached version is thread-safe
  }


  /**
   * Key used in cache for member lookup by path
   */
  @Value.Immutable(prehash = true)
  interface ClassAndPath {
    @Value.Parameter
    Class<?> type();

    @Value.Parameter
    String path();
  }

}
