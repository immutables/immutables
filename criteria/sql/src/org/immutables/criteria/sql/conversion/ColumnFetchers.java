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
