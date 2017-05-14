package org.immutables.gson.adapter;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import java.util.Map;

/**
 *
 */
@Gson.TypeAdapters(nullAsDefault = true, emptyAsNulls = true)
@Value.Immutable
public interface MapTest {
  Map<String, Boolean> mapBoolean();

  Map<String, Object> mapObject();

  Map<String, Double> mapDouble();
}
