package org.immutables.fixture.serial;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Style(allParameters = true)
@Serial.AllStructural
public @interface StructStyle {}
