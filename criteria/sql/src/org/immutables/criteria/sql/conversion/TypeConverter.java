package org.immutables.criteria.sql.conversion;

@FunctionalInterface
public interface TypeConverter<F, T> {
    T apply(final F value);
}