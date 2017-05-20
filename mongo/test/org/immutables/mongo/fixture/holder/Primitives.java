package org.immutables.mongo.fixture.holder;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * To test the bug with embedded primitives
 */
@Value.Immutable
@JsonSerialize(as = ImmutablePrimitives.class)
@JsonDeserialize(as = ImmutablePrimitives.class)
@Gson.TypeAdapters
public interface Primitives {

    boolean booleanValue();

    byte byteValue();

    short shortValue();

    int intValue();

    long longValue();

    float floatValue();

    double doubleValue();
}
