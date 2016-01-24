package org.immutables.fixture.jdkonly;

import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.immutables.gson.Gson;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Serial.Structural
@Value.Immutable(builder = false, singleton = true, intern = true, prehash = true)
@Gson.TypeAdapters
public interface JdkOptionals {
  @Value.Parameter
  Optional<String> v2();

  @Value.Parameter
  OptionalInt i1();

  @Value.Parameter
  OptionalLong l1();

  @Value.Parameter
  OptionalDouble d1();
}

@Value.Immutable
@Value.Style(privateNoargConstructor = true)
interface JdkOptionalsSer extends Serializable {
  @Value.Parameter
  Optional<String> v2();

  @Value.Parameter
  OptionalInt i1();

  @Value.Parameter
  OptionalLong l1();

  @Value.Parameter
  OptionalDouble d1();
}
