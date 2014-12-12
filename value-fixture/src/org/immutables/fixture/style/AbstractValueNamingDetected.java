package org.immutables.fixture.style;

import java.util.Set;
import org.immutables.value.Value;

/**
 * Feature combination
 * <ul>
 * <li>Abstract type and accessor name detection
 * <li>Generated builder naming customization
 * </ul>
 */
@Value.Immutable(copy = true)
@Value.Style(
    get = {"extract*", "collect*"},
    typeAbstract = "Abstract*",
    typeImmutable = "*",
    build = "build*",
    init = "using*",
    add = "with*Appended",
    builder = "newBuilder")
abstract class AbstractValueNamingDetected {

  abstract int extractVal();

  abstract Set<String> collectStr();

  void use() {
    ValueNamingDetected.newBuilder()
        .usingVal(1)
        .withStrAppended("Appended!")
        .buildValueNamingDetected();
  }
}
