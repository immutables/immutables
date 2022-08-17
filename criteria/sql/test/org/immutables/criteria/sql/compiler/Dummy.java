package org.immutables.criteria.sql.compiler;

import org.immutables.criteria.Criteria;
import org.immutables.value.Value;

@Criteria
@Value.Immutable
public interface Dummy {
    @Criteria.Id
    int id();

    String name();
}
