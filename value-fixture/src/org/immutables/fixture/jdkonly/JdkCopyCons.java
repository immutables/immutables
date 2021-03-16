package org.immutables.fixture.jdkonly;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true)
public interface JdkCopyCons {

    @Value.Parameter
    List<String> getStrings();

}
