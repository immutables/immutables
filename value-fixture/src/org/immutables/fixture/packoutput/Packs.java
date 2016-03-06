package org.immutables.fixture.packoutput;

import org.immutables.mongo.Mongo;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

// Compilation test for customization of output package
// Tries to use many different generators
@Value.Style(packageGenerated = "b*.impl")
@Value.Immutable
@Value.Enclosing
@Gson.TypeAdapters
@Mongo.Repository
public abstract class Packs {
  public abstract Perk perk();

  @Value.Immutable
  public interface Perk {}

  void use() {
    borg.immutables.fixture.packoutput.impl.ImmutablePacks.builder().build();
    borg.immutables.fixture.packoutput.impl.ImmutablePacks.Perk.builder().build();
    borg.immutables.fixture.packoutput.impl.PacksRepository.class.toString();
    borg.immutables.fixture.packoutput.impl.GsonAdaptersPacks.class.toString();
  }
}
