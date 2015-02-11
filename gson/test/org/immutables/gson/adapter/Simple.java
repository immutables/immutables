package org.immutables.gson.adapter;

import java.util.List;
import javax.annotation.Nullable;
import com.google.common.base.Optional;
import org.immutables.value.Value;
import org.immutables.gson.Gson;

@Gson.TypeAdapters(emptyAsNulls = true, fieldNamingStrategy = true)
@Value.Immutable
public interface Simple {
  Optional<String> optionalString();

  @Gson.Named("_nullable_")
  @Nullable
  Integer nlb();

  List<Character> characterList();
}
