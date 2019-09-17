package org.immutables.fixture.modifiable;

import java.util.Collections;
import java.util.Map;
import org.immutables.value.Value;

@Value.Style(jdkOnly = true)
@Value.Immutable
@Value.Modifiable
public interface DefaultMap {
    @Value.Default
    default Map<String, String> getMap() {
        return Collections.emptyMap();
    }
}
