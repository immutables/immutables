package org.immutables.criteria.sql.reflection;

import org.immutables.criteria.sql.conversion.ColumnMapping;
import org.immutables.value.Value;

@Value.Immutable
public interface SQLPropertyMetadata {
    String name();

    Class type();

    ColumnMapping mapping();

    PropertyExtractor extractor();
}
