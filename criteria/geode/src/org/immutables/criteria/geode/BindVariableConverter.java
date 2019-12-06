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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This function is used for OQLs of type
 * {@code select * form /region where id in $1} (where {@code $1} is a guava collection).
 * Helpful in cases when client has guava library but server has not.
 *
 * <p>By default, Geode can't deserialize Guava collections (and other guava classes) since guava might
 * not be on servers classpath. Bind variables in OQL are serialized using standard java serialization.
 *
 * <p>Manually "clone" collections into a standard java collection implementation
 * like {@link ArrayList} or {@link HashSet}.
 */
class BindVariableConverter implements Function<Object, Object> {

  @Override
  public Object apply(Object value) {
    if (!(value instanceof Iterable)) {
      return value;
    }

    if (value instanceof HashSet || value instanceof ArrayList || value instanceof TreeSet) {
      // don't convert java collections (assume no guava implementations exists inside it)
      return value;
    }

    // transform to java collections
    Collector<Object, ?, ? extends Iterable<Object>> collector;
    if (value instanceof SortedSet) {
      collector = Collectors.toCollection(TreeSet::new);
    } else if (value instanceof Set) {
      collector = Collectors.toCollection(HashSet::new);
    } else {
      collector = Collectors.toList();
    }

    return StreamSupport.stream(((Iterable<Object>) value).spliterator(), false).map(this::apply).collect(collector);
  }

}
