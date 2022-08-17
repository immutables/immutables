package org.immutables.criteria.sql.conversion;

import org.immutables.value.Value;

@Value.Immutable
public interface ColumnMapping {
    String name();

    Class type();

    ColumnFetcher<?> fetcher();
}
