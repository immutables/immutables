package org.immutables.criteria.micrometer;

import org.immutables.criteria.backend.Backend;

import io.micrometer.core.instrument.Tag;

public interface TagGenerator {

    Iterable<Tag> generate(Backend.Operation operation);

}
