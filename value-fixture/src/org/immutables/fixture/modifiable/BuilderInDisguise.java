package org.immutables.fixture.modifiable;

import java.util.List;

import org.immutables.value.Value;

// from https://github.com/immutables/immutables/issues/234#issuecomment-170574928
// which is referenced in the docs

@Value.Style(
    typeModifiable = "*Builder", // modifiable will be generated as "FooBuilder"
    toImmutable = "build",  // rename "toImmutable" method to "build"
    set = "*", // adjust to your taste: setters with no prefixes like in default builders.
    create = "new" // plain constructor to create builder, just as example
)  // style could also put on package or as meta annotation
@Value.Immutable
@Value.Modifiable
public abstract class BuilderInDisguise {
  public abstract String getName();
  public abstract List<Integer> getCounts();
  public static BuilderInDisguiseBuilder builder() {  // just for convenience, can create builder directly
    return new BuilderInDisguiseBuilder();
  }
}