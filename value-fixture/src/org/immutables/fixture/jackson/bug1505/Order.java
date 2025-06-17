package org.immutables.fixture.jackson.bug1505;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(overshadowImplementation = true,
        jdkOnly = true, strictBuilder = true)
@JsonDeserialize(builder = Order.Builder.class)
public interface Order {

    @JsonProperty("id")
    long getId();

    @JsonProperty("lines")
    List<OrderLine> getOrderLines();

    static Builder builder() {
        return new Builder();
    }

    class Builder extends ImmutableOrder.Builder {}
}
