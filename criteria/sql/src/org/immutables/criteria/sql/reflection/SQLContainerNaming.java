package org.immutables.criteria.sql.reflection;

import org.immutables.criteria.backend.ContainerNaming;
import org.immutables.criteria.sql.SQL;

import java.util.Objects;

public interface SQLContainerNaming extends ContainerNaming {
    ContainerNaming FROM_SQL_TABLE_ANNOTATION = clazz -> {
        Objects.requireNonNull(clazz, "clazz");
        final SQL.Table annotation = clazz.getAnnotation(SQL.Table.class);
        if (annotation == null || annotation.value().isEmpty()) {
            throw new UnsupportedOperationException(String.format("%s.name annotation is not defined on %s",
                    SQL.Table.class.getSimpleName(), clazz.getName()));
        }
        return annotation.value();
    };

    ContainerNaming SQL = clazz -> {
        try {
            return FROM_SQL_TABLE_ANNOTATION.name(clazz);
        } catch (UnsupportedOperationException u) {
            try {
                return FROM_REPOSITORY_ANNOTATION.name(clazz);
            } catch (UnsupportedOperationException e) {
                return FROM_CLASSNAME.name(clazz);
            }
        }
    };
}
