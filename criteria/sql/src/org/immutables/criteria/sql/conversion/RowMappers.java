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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.immutables.criteria.sql.reflection.SQLPropertyMetadata;
import org.immutables.criteria.sql.reflection.SQLTypeMetadata;
import org.immutables.criteria.sql.util.TypeKeyHashMap;

import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RowMappers {
    private static final ObjectMapper MAPPER;
    private static final TypeKeyHashMap<RowMapper<?>> MAPPER_CACHE;

    static {
        MAPPER = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule());
        MAPPER_CACHE = new TypeKeyHashMap<>();
        register(String.class, r -> r.getString(1));
        register(byte.class, r -> r.getByte(1));
        register(Byte.class, r -> r.getByte(1));
        register(short.class, r -> r.getShort(1));
        register(Short.class, r -> r.getShort(1));
        register(int.class, r -> r.getInt(1));
        register(Integer.class, r -> r.getInt(1));
        register(long.class, r -> r.getLong(1));
        register(Long.class, r -> r.getLong(1));
        register(float.class, r -> r.getFloat(1));
        register(Float.class, r -> r.getFloat(1));
        register(double.class, r -> r.getDouble(1));
        register(Double.class, r -> r.getDouble(1));
    }

    public static <T> void register(final Class<T> type, final RowMapper<T> mapper) {
        MAPPER_CACHE.put(type, mapper);
    }

    @SuppressWarnings("unchecked")
    public static <T> RowMapper<T> get(final Class<T> clazz) {
        return (RowMapper<T>) MAPPER_CACHE.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> RowMapper<T> get(final SQLTypeMetadata metadata) {
        return (RowMapper<T>) MAPPER_CACHE.computeIfAbsent(metadata.type(), t -> newRowMapper(metadata));
    }

    @SuppressWarnings("unchecked")
    private static <T> RowMapper<T> newRowMapper(final SQLTypeMetadata metadata) {
        return row -> {
            final Map<String, Object> data = new HashMap<>();
            final ResultSetMetaData rm = row.getMetaData();
            for (int i = 1; i <= rm.getColumnCount(); ++i) {
                final String name = rm.getColumnLabel(i).toLowerCase(Locale.ROOT);
                final SQLPropertyMetadata property = metadata.columns().get(name);
                if (property != null) {
                    data.put(property.name(),
                            TypeConverters.convert(property.mapping().type(),
                                    property.type(),
                                    property.mapping().fetcher().apply(row, i)));
                }
            }
            return (T) MAPPER.convertValue(data, metadata.type());
        };
    }
}
