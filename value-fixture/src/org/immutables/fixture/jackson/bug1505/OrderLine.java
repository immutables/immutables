package org.immutables.fixture.jackson.bug1505;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(overshadowImplementation = true,
        jdkOnly = true, strictBuilder = true)
@JsonDeserialize(builder = OrderLine.Builder.class)
public interface OrderLine {

    @JsonProperty("id")
    long getId();

    @JsonProperty("description")
    String description();

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableOrderLine.Builder {}
}
