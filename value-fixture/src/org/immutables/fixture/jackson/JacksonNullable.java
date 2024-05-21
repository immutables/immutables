package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableJacksonNullable.class)
@JsonDeserialize(builder = ImmutableJacksonNullable.Builder.class)
public interface JacksonNullable {

    String notNullable();

    @Nullable
    String nullable();
}
