package org.immutables.mongo.fixture.holder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.value.Value;

/**
 * Data object which can store heterogeneous types
 */
@Gson.TypeAdapters
@Mongo.Repository("holder")
@Value.Immutable
@JsonSerialize(as = ImmutableHolder.class)
@JsonDeserialize(as = ImmutableHolder.class)
public interface Holder {

    String TYPE_PROPERTY = "@class";


    @Mongo.Id
    String id();

    /**
     * Class name is encoded as JSON attribute ({@code @class}
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = TYPE_PROPERTY)
    Object value();
}
