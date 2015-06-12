package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class SerialMirrors {
  private SerialMirrors() {}

  @Mirror.Annotation("org.immutables.serial.Serial.Version")
  public @interface Version {
    long value();
  }

  @Mirror.Annotation("org.immutables.serial.Serial.Structural")
  public @interface Structural {}
}
