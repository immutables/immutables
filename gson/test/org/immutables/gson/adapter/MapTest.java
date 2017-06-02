package org.immutables.gson.adapter;

import java.util.Map;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

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
