package org.immutables.fixture.modifiable.nested;


import org.immutables.fixture.modifiable.Companion;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
public interface ReferenceDifferentPackage {

	Companion companion();
}
