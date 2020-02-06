package org.immutables.criteria.geode;

import java.util.Collections;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.micrometer.TagGenerator;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * Generates {@link Tag} containing OQL query
 */
public class OqlTagGenerator implements TagGenerator {

    private final OqlGenerator oqlGenerator;

    public OqlTagGenerator(Class<?> entityType) {
        this.oqlGenerator = ImmutableOqlGenerator.builder()
                .regionName(entityType.getSimpleName().toLowerCase())
                .pathNaming(ReservedWordNaming.of(PathNaming.defaultNaming()))
                .useBindVariables(true)
                .build();
    }

    @Override
    public Iterable<Tag> generate(Backend.Operation operation) {
        if (operation instanceof StandardOperations.Select) {
            final Query query = ((StandardOperations.Select) operation).query();
            final Oql oql = oqlGenerator.generate(query);
            return Tags.of("query", oql.oql());
        } else {
            return Collections.emptyList();
        }
    }
}
