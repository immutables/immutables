package org.immutables.gson.adapter;

import org.immutables.value.Value;
import com.google.common.base.Optional;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Value.Immutable
public interface Unsimple {
  Optional<String> optionalString();

  @Gson.Named("_nullable_")
  @Nullable
  Integer nlb();

  List<Character> characterList();
}
