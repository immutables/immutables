package org.immutables.criteria.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A set of annotations specific to the SQL backend
 */
public @interface SQL {
    /**
     * Used to define the table an entity is mapped to. If not defined the table name will be the same as
     * {@code Class.getSimpleName() }
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Table {
        String value() default "";
    }

    /**
     * Used to map a property to a column and the target column type. This will drive the use of
     * {@code TypeConverters.convert()} for mapping values to/from the database.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Column {
        String name() default "";

        Class<?> type();
    }
}
