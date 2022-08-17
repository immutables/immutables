package org.immutables.criteria.sql.reflection;

@FunctionalInterface
public interface PropertyExtractor {
    Object extract(Object entity);
}
