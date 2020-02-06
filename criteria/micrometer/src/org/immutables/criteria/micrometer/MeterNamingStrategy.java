package org.immutables.criteria.micrometer;

import java.util.Objects;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.StandardOperations;

public class MeterNamingStrategy {

    private static final String CRITERIA = "criteria";
    private static final String OPERATIONS = "operations";

    private final Backend backend;

    public MeterNamingStrategy(Backend backend) {
        this.backend = backend;
    }

    public String getMeterName(Backend.Operation operation) {
        Objects.requireNonNull(operation, "operation");
        return String.join(
                ".",
                CRITERIA, backend.name().toLowerCase(), OPERATIONS, toName(operation)
        );
    }

    private static String toName(Backend.Operation operation) {
        return typeOf(operation).getSimpleName().toLowerCase();
    }

    private static Class<? extends Backend.Operation> typeOf(Backend.Operation operation) {
        if (operation instanceof StandardOperations.Select) {
            return StandardOperations.Select.class;
        } else if (operation instanceof StandardOperations.Insert) {
            return StandardOperations.Insert.class;
        } else if (operation instanceof StandardOperations.Update) {
            return StandardOperations.Update.class;
        } else if (operation instanceof StandardOperations.UpdateByQuery) {
            return StandardOperations.UpdateByQuery.class;
        } else if (operation instanceof StandardOperations.Delete) {
            return StandardOperations.Delete.class;
        } else {
            throw new IllegalArgumentException("Unsupported Operation " + operation.getClass().getName());
        }
    }
}
