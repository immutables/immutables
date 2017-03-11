package org.immutables.gson.adapter;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
@Value.Style(stagedBuilder = true)
public interface StagedBuilderVal {
  int value();

  String string();
}
