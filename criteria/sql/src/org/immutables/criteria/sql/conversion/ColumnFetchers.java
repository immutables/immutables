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
package org.immutables.criteria.sql.conversion;

import org.immutables.criteria.sql.util.TypeKeyHashMap;

import java.sql.ResultSet;
import java.util.UUID;

public class ColumnFetchers {
  private static final TypeKeyHashMap<ColumnFetcher<?>> FETCHERS;

  static {
    FETCHERS = new TypeKeyHashMap<>();
    register(boolean.class, ResultSet::getBoolean);
    register(byte.class, ResultSet::getByte);
    register(short.class, ResultSet::getShort);
    register(int.class, ResultSet::getInt);
    register(long.class, ResultSet::getLong);
    register(float.class, ResultSet::getFloat);
    register(double.class, ResultSet::getDouble);
    register(Boolean.class, ResultSet::getBoolean);
    register(Byte.class, ResultSet::getByte);
    register(Short.class, ResultSet::getShort);
    register(Integer.class, ResultSet::getInt);
    register(Long.class, ResultSet::getLong);
    register(Float.class, ResultSet::getFloat);
    register(Double.class, ResultSet::getDouble);
    register(String.class, ResultSet::getString);
    register(UUID.class, (r, i) -> UUID.fromString(r.getString(i)));
  }

  public static <T> void register(final Class<T> type, final ColumnFetcher<T> fetcher) {
    FETCHERS.put(type, fetcher);
  }

  public static ColumnFetcher get(final Class<?> type) {
    return FETCHERS.get(type);
  }
}
