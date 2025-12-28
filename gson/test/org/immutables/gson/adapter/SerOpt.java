package org.immutables.gson.adapter;

import java.time.Instant;
import java.util.Optional;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface SerOpt {
  Optional<Instant> instant();
}
