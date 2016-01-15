package org.immutables.fixture.jackson;

import org.immutables.value.Value;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableFoo.class)
@JsonDeserialize(as = ImmutableFoo.class)
public abstract class Foo {

    @JsonProperty("bar")
    public abstract boolean isBar();
}