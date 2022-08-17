/*
 * Copyright 2022 Immutables Authors and Contributors
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
package org.immutables.criteria.sql.util;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("serial")
public class TypeKeyHashMap<V> extends HashMap<Class<?>, V> {
    @Override
    public boolean containsKey(final Object key) {
        return findEntry((Class<?>) key).isPresent();
    }

    @Override
    public V get(final Object key) {
        final Optional<Entry<Class<?>, V>> entry = findEntry((Class<?>) key);
        return entry.map(Entry::getValue).orElse(null);
    }

    private Optional<Entry<Class<?>, V>> findEntry(final Class<?> key) {
        return entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(key))
                .findFirst();
    }
}